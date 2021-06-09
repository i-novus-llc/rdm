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
public class UnversionedAddRowValuesStrategy implements AddRowValuesStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Autowired
    @Qualifier("defaultAddRowValuesStrategy")
    private AddRowValuesStrategy addRowValuesStrategy;

    @Override
    public void add(RefBookVersionEntity entity, List<RowValue> rowValues) {

        addRowValuesStrategy.add(entity, rowValues);

        processReferrers(entity, rowValues);
    }

    private void processReferrers(RefBookVersionEntity entity, List<RowValue> rowValues) {

        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        if (primaries.isEmpty())
            return;

        // Для поиска существующих конфликтов нужны сохранённые значения добавленных записей.
        Collection<RowValue> addedRowValues = findAddedRowValues(entity, rowValues, primaries);
        processReferrers(entity, primaries, addedRowValues);
    }

    protected void processReferrers(RefBookVersionEntity entity, List<Structure.Attribute> primaries,
                                    Collection<RowValue> addedRowValues) {
        if (isEmpty(addedRowValues))
            return;

        Map<String, RowValue> referredRowValues = RowUtils.toReferredRowValues(primaries, addedRowValues);
        if (isEmpty(referredRowValues))
            return;

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, referredRowValues)
                )
        );
    }

    private Collection<RowValue> findAddedRowValues(RefBookVersionEntity entity, List<RowValue> rowValues,
                                                    List<Structure.Attribute> primaries) {
        
        StorageDataCriteria dataCriteria = toEntityDataCriteria(entity, rowValues, primaries);
        return searchDataService.getPagedData(dataCriteria).getCollection();
    }

    private StorageDataCriteria toEntityDataCriteria(RefBookVersionEntity entity, List<RowValue> rowValues,
                                                     List<Structure.Attribute> primaries) {

        Set<List<FieldSearchCriteria>> primarySearchCriterias = toPrimarySearchCriterias(rowValues, primaries);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                entity.getStorageCode(), // Без учёта локализации
                entity.getFromDate(), entity.getToDate(),
                ConverterUtil.fields(entity.getStructure()), primarySearchCriterias, null);
        dataCriteria.setPage(BaseDataCriteria.MIN_PAGE);
        dataCriteria.setSize(rowValues.size());

        return dataCriteria;
    }

    private Set<List<FieldSearchCriteria>> toPrimarySearchCriterias(List<RowValue> rowValues,
                                                                    List<Structure.Attribute> primaries) {
        return rowValues.stream()
                .map(rowValue -> toPrimarySearchCriterias(rowValue, primaries))
                .collect(toSet());
    }

    private List<FieldSearchCriteria> toPrimarySearchCriterias(RowValue rowValue,
                                                               List<Structure.Attribute> primaries) {
        return primaries.stream()
                .map(primary ->
                        ConverterUtil.toFieldSearchCriteria(primary.getCode(), primary.getType(),
                                SearchTypeEnum.EXACT, singletonList(RowUtils.toSearchValue(primary, rowValue)))
                ).collect(toList());
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer  сущность-версия, ссылающаяся на текущий справочник
     * @param entity    сущность-версия, на которую есть ссылки
     * @param rowValues набор добавляемых записей в entity
     */
    private void processReferrer(RefBookVersionEntity referrer, RefBookVersionEntity entity,
                                 Map<String, RowValue> rowValues) {

        String refBookCode = entity.getRefBook().getCode();
        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);

        // storageCode - Без учёта локализации
        ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references,
                referrer.getStorageCode(), new ArrayList<>(rowValues.keySet()));
        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page ->

            // При наличии конфликта DELETED:
            // если запись восстановлена - удалить конфликт,
            // иначе - заменить тип конфликта на UPDATED.
            recalculateDataConflicts(referrer, rowValues, references, page.getCollection())
        );
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer,
                                          Map<String, RowValue> addedRowValues,
                                          List<Structure.Reference> references,
                                          Collection<? extends RowValue> refRowValues) {
        references.forEach(reference ->
                recalculateDataConflicts(referrer, addedRowValues, reference, refRowValues)
        );
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer,
                                          Map<String, RowValue> addedRowValues,
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

        // Определить действия над конфликтами по результату сравнения отображаемых значений.
        List<RefBookConflictEntity> toUpdate = new ArrayList<>(conflicts.size());
        List<RefBookConflictEntity> toDelete = new ArrayList<>(conflicts.size());

        for (RefBookConflictEntity conflict : conflicts) {

            Reference fieldReference = RowUtils.getFieldReference(refRowValues, conflict.getRefRecordId(), referenceCode);
            RowValue addedRowValue = (fieldReference != null) ? addedRowValues.get(fieldReference.getValue()) : null;
            if (addedRowValue == null) continue;

            String newDisplayValue = FieldValueUtils.toDisplayValue(
                    reference.getDisplayExpression(), addedRowValue, null);

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
}
