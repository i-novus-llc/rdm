package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.FieldValueUtils;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.CollectionPageIterator;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toReferenceSearchCriterias;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedAddRowValuesStrategy extends DefaultAddRowValuesStrategy {

    private static final int REF_BOOK_VERSION_DATA_PAGE_SIZE = 100;

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Override
    protected void after(RefBookVersionEntity entity, List<RowValue> rowValues) {

        super.after(entity, rowValues);

        processReferrers(entity, rowValues);
    }

    private void processReferrers(RefBookVersionEntity entity, List<RowValue> rowValues) {

        // Для поиска существующих конфликтов нужны сохранённые значения добавленных записей.
        Collection<RowValue> addedRowValues = findAddedRowValues(entity, rowValues);
        if (isEmpty(addedRowValues))
            return;

        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        List<String> primaryValues = RowUtils.toReferenceValues(primaries, addedRowValues);

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, primaryValues, addedRowValues)
                )
        );
    }

    private Collection<RowValue> findAddedRowValues(RefBookVersionEntity entity, List<RowValue> rowValues) {
        
        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        if (primaries.isEmpty())
            return Collections.emptyList(); // Нет первичных ключей, нет и ссылок

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
     * @param referrer      сущность-версия, ссылающаяся на текущий справочник
     * @param entity        сущность-версия, на которую есть ссылки
     * @param primaryValues значения первичных ключей записей
     * @param rowValues     добавляемые записи в entity
     */
    private void processReferrer(RefBookVersionEntity referrer, RefBookVersionEntity entity,
                                 List<String> primaryValues, Collection<RowValue> rowValues) {

        String refBookCode = entity.getRefBook().getCode();
        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);

        StorageDataCriteria dataCriteria = toReferrerDataCriteria(referrer, references, primaryValues);
        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page ->

            // При наличии конфликта DELETED:
            // если запись восстановлена - удалить конфликт,
            // иначе - заменить тип конфликта на UPDATED.
            recalculateDataConflicts(referrer, primaries, rowValues, references, page.getCollection())
        );
    }

    private StorageDataCriteria toReferrerDataCriteria(RefBookVersionEntity referrer,
                                                       List<Structure.Reference> references,
                                                       List<String> primaryValues) {

        List<Field> referenceFields = references.stream().map(ConverterUtil::field).collect(toList());
        Set<List<FieldSearchCriteria>> fieldSearchCriterias = toReferenceSearchCriterias(references, primaryValues);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                referrer.getStorageCode(), // Без учёта локализации
                referrer.getFromDate(), referrer.getToDate(),
                referenceFields, fieldSearchCriterias, null);
        dataCriteria.setPage(BaseDataCriteria.MIN_PAGE);
        dataCriteria.setSize(REF_BOOK_VERSION_DATA_PAGE_SIZE);

        return dataCriteria;
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer,
                                          List<Structure.Attribute> primaries,
                                          Collection<RowValue> addedRowValues,
                                          List<Structure.Reference> references,
                                          Collection<? extends RowValue> refRowValues) {
        references.forEach(reference ->
                recalculateDataConflicts(referrer, primaries, addedRowValues, reference, refRowValues)
        );
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer,
                                          List<Structure.Attribute> primaries,
                                          Collection<RowValue> addedRowValues,
                                          Structure.Reference reference,
                                          Collection<? extends RowValue> refRowValues) {

        // Найти существующие конфликты DELETED для ссылки.
        List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);
        String referenceCode = reference.getAttribute();
        List<RefBookConflictEntity> conflicts =
                conflictRepository.findByReferrerVersionIdAndRefRecordIdInAndRefFieldCodeAndConflictType(
                referrer.getId(), refRecordIds, referenceCode, ConflictType.DELETED
        );

        // Определить действия над конфликтами по ссылке и записям.
        List<RefBookConflictEntity> toDelete = new ArrayList<>(conflicts.size());
        List<RefBookConflictEntity> toUpdate = new ArrayList<>(conflicts.size());

        for (RefBookConflictEntity conflict : conflicts) {
            Reference fieldReference = getFieldReference(refRowValues, conflict.getRefRecordId(), referenceCode);
            if (fieldReference == null) continue;

            String addedDisplayValue = buildDisplayValue(
                    primaries, addedRowValues, reference, fieldReference.getValue()
            );

            if (Objects.equals(fieldReference.getDisplayValue(), addedDisplayValue))
                toDelete.add(conflict);
            else
                toUpdate.add(conflict);
        }

        // Выполнить действия над конфликтами.
        if (!isEmpty(toDelete)) {
            conflictRepository.deleteAll(toDelete);
        }

        if (!isEmpty(toUpdate)) {
            toUpdate.forEach(conflict -> conflict.setConflictType(ConflictType.UPDATED));
            conflictRepository.saveAll(toUpdate);
        }
    }

    private Reference getFieldReference(Collection<? extends RowValue> rowValues,
                                        Long systemId, String referenceCode) {

        RowValue conflictedRowValue = rowValues.stream()
                .filter(rowValue -> Objects.equals(rowValue.getSystemId(), systemId))
                .findFirst().orElse(null);
        if (conflictedRowValue == null)
            return null;

        FieldValue fieldValue = conflictedRowValue.getFieldValue(referenceCode);
        return (fieldValue instanceof ReferenceFieldValue)
                ? ((ReferenceFieldValue) fieldValue).getValue()
                : null;
    }

    private String buildDisplayValue(List<Structure.Attribute> primaries, Collection<RowValue> addedRowValues,
                                     Structure.Reference reference, String referenceValue) {

        RowValue referredRowValue = addedRowValues.stream()
                .filter(rowValue -> Objects.equals(RowUtils.toReferenceValue(primaries, rowValue), referenceValue))
                .findFirst().orElse(null);
        if (referredRowValue == null)
            return null;

        return FieldValueUtils.toDisplayValue(reference.getDisplayExpression(), referredRowValue, null);
    }
}
