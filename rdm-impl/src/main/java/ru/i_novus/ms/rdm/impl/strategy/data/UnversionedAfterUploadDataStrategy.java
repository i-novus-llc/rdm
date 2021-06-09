package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.FieldValueUtils;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferrerDataCriteria;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.CollectionPageIterator;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedAfterUploadDataStrategy implements AfterUploadDataStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Autowired
    @Qualifier("defaultAfterUploadDataStrategy")
    private AfterUploadDataStrategy afterUploadDataStrategy;

    @Override
    public void apply(RefBookVersionEntity entity) {

        afterUploadDataStrategy.apply(entity);

        processReferrers(entity);
    }

    private void processReferrers(RefBookVersionEntity entity) {

        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        if (primaries.isEmpty())
            return;

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, primaries)
                )
        );
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer сущность-версия, ссылающаяся на текущий справочник
     * @param entity   сущность-версия, на которую есть ссылки
     */
    private void processReferrer(RefBookVersionEntity referrer, RefBookVersionEntity entity,
                                 List<Structure.Attribute> primaries) {

        String refBookCode = entity.getRefBook().getCode();
        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);

        // storageCode - Без учёта локализации
        ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references, referrer.getStorageCode(), null);
        dataCriteria.setFieldFilters(ConverterUtil.toNotNullSearchCriterias(references));

        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page ->

            // При наличии конфликта DELETED:
            // если запись восстановлена - удалить конфликт,
            // иначе - заменить тип конфликта на UPDATED.
            recalculateDataConflicts(referrer, entity, primaries, references, page.getCollection())
        );
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer,
                                          RefBookVersionEntity entity,
                                          List<Structure.Attribute> primaries,
                                          List<Structure.Reference> references,
                                          Collection<? extends RowValue> refRowValues) {
        references.forEach(reference ->
                recalculateDataConflicts(referrer, entity, primaries, reference, refRowValues)
        );
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer,
                                          RefBookVersionEntity entity,
                                          List<Structure.Attribute> primaries,
                                          Structure.Reference reference,
                                          Collection<? extends RowValue> refRowValues) {

        // Найти существующие конфликты DELETED для текущей ссылки.
        List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);
        String referenceCode = reference.getAttribute();
        List<RefBookConflictEntity> conflicts =
                conflictRepository.findByReferrerVersionIdAndRefRecordIdInAndRefFieldCodeAndConflictType(
                referrer.getId(), refRecordIds, referenceCode, ConflictType.DELETED
        );
        if (isEmpty(conflicts))
            return;

        Collection<RowValue> rowValues = findReferredRowValues(entity, primaries, referenceCode, refRowValues);
        Map<String, RowValue> referredRowValues = RowUtils.toReferredRowValues(primaries, rowValues);

        // Определить действия над конфликтами по результату сравнения отображаемых значений.
        List<RefBookConflictEntity> toUpdate = new ArrayList<>(conflicts.size());
        List<RefBookConflictEntity> toDelete = new ArrayList<>(conflicts.size());

        for (RefBookConflictEntity conflict : conflicts) {

            Reference fieldReference = RowUtils.getFieldReference(refRowValues, conflict.getRefRecordId(), referenceCode);
            RowValue referredRowValue = (fieldReference != null) ? referredRowValues.get(fieldReference.getValue()) : null;
            if (referredRowValue == null) continue;

            String newDisplayValue = FieldValueUtils.toDisplayValue(
                    reference.getDisplayExpression(), referredRowValue, null);

            if (Objects.equals(fieldReference.getDisplayValue(), newDisplayValue)) {

                // Восстановление записи для ссылки:
                toDelete.add(conflict); // удалить конфликт

            } else {
                // Добавление изменённой записи:
                conflict.setConflictType(ConflictType.UPDATED);
                toUpdate.add(conflict); // сохранить UPDATED-конфликт
            }
        }

        // Выполнить действия над конфликтами.
        if (!isEmpty(toUpdate)) {
            conflictRepository.saveAll(toUpdate);
        }

        if (!isEmpty(toDelete)) {
            conflictRepository.deleteAll(toDelete);
        }
    }

    private Collection<RowValue> findReferredRowValues(RefBookVersionEntity entity,
                                                       List<Structure.Attribute> primaries,
                                                       String referenceCode,
                                                       Collection<? extends RowValue> refRowValues) {

        List<String> referenceValues = refRowValues.stream()
                .map(rowValue -> RowUtils.getFieldReferenceValue(rowValue, referenceCode))
                .filter(Objects::nonNull)
                .distinct().collect(toList());

        StorageDataCriteria dataCriteria = toEntityDataCriteria(entity, primaries, referenceValues);
        return searchDataService.getPagedData(dataCriteria).getCollection();
    }

    private StorageDataCriteria toEntityDataCriteria(RefBookVersionEntity entity,
                                                     List<Structure.Attribute> primaries,
                                                     List<String> referenceValues) {

        Set<List<FieldSearchCriteria>> primarySearchCriterias = toPrimarySearchCriterias(primaries, referenceValues);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                entity.getStorageCode(), // Без учёта локализации
                entity.getFromDate(), entity.getToDate(),
                ConverterUtil.fields(entity.getStructure()), primarySearchCriterias, null);
        dataCriteria.setPage(BaseDataCriteria.MIN_PAGE);
        dataCriteria.setSize(referenceValues.size());

        return dataCriteria;
    }

    private Set<List<FieldSearchCriteria>> toPrimarySearchCriterias(List<Structure.Attribute> primaries,
                                                                    List<String> referenceValues) {
        return referenceValues.stream()
                .map(refValue -> toPrimarySearchCriterias(primaries, refValue))
                .collect(toSet());
    }

    private List<FieldSearchCriteria> toPrimarySearchCriterias(List<Structure.Attribute> primaries,
                                                               String referenceValue) {
        // На данный момент первичным ключом может быть только одно поле.
        // Ссылка на значение составного ключа невозможна.
        Structure.Attribute primary = primaries.get(0);

        return singletonList(
                ConverterUtil.toFieldSearchCriteria(primary.getCode(), primary.getType(),
                        SearchTypeEnum.EXACT, singletonList(referenceValue)
                )
        );
    }
}
