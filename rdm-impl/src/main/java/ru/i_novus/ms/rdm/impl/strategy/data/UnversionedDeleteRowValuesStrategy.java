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
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.CollectionPageIterator;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedDeleteRowValuesStrategy extends DefaultDeleteRowValuesStrategy {

    private static final int REF_BOOK_VERSION_DATA_PAGE_SIZE = 100;

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Override
    protected void before(RefBookVersionEntity entity, List<Object> systemIds) {

        super.before(entity, systemIds);

        processReferrers(entity, systemIds);
    }

    private void processReferrers(RefBookVersionEntity entity, List<Object> systemIds) {

        // Для поиска записей-ссылок нужны не systemId, а строковые значения первичных ключей.
        List<String> primaryValues = findPrimaryValues(entity, systemIds);

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, primaryValues)
                )
        );
    }

    private List<String> findPrimaryValues(RefBookVersionEntity entity, List<Object> systemIds) {

        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        StorageDataCriteria dataCriteria = toEntityDataCriteria(entity, systemIds, primaries);

        Collection<RowValue> rowValues = searchDataService.getPagedData(dataCriteria).getCollection();
        return toReferenceValues(primaries, rowValues);
    }

    private StorageDataCriteria toEntityDataCriteria(RefBookVersionEntity entity, List<Object> systemIds,
                                                     List<Structure.Attribute> primaries) {
        List<Field> primaryFields = primaries.stream()
                .map(primary -> ConverterUtil.field(primary.getCode(), primary.getType()))
                .collect(toList());

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                entity.getStorageCode(), // Без учёта локализации
                entity.getFromDate(), entity.getToDate(),
                primaryFields);
        dataCriteria.setSystemIds(RowUtils.toLongSystemIds(systemIds));

        dataCriteria.setPage(BaseDataCriteria.MIN_PAGE);
        dataCriteria.setSize(systemIds.size());

        return dataCriteria;
    }

    private List<String> toReferenceValues(List<Structure.Attribute> primaries, Collection<RowValue> rowValues) {

        return rowValues.stream().map(rowValue -> toReferenceValue(primaries, rowValue)).collect(toList());
    }

    private String toReferenceValue(List<Structure.Attribute> primaries, RowValue rowValue) {

        // На данный момент первичным ключом может быть только одно поле.
        // Ссылка на значение составного ключа невозможна.
        FieldValue fieldValue = rowValue.getFieldValue(primaries.get(0).getCode());
        Serializable value = FieldValueUtils.castFieldValue(fieldValue, FieldType.STRING);

        return value != null ? value.toString() : null;
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer      сущность-версия, ссылающаяся на текущий справочник
     * @param entity        сущность-версия, на которую есть ссылки
     * @param primaryValues список значений первичных ключей записей
     */
    private void processReferrer(RefBookVersionEntity referrer,
                                 RefBookVersionEntity entity, List<String> primaryValues) {

        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(entity.getRefBook().getCode());
        List<String> referenceCodes = references.stream().map(Structure.Reference::getAttribute).collect(toList());

        StorageDataCriteria dataCriteria = toReferrerDataCriteria(referrer, references, primaryValues);
        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page -> {

            // Удалить существующие конфликты для найденных записей.
            deleteReferrerConflicts(referrer, page.getCollection());

            // Если есть значение ссылки на один из systemIds, создать конфликт DELETED.
            List<RefBookConflictEntity> entities = recalculateDataConflicts(referrer, entity,
                    referenceCodes, primaryValues, page.getCollection());
            if (!isEmpty(entities)) {
                getConflictRepository().saveAll(entities);
            }
        });
    }

    private StorageDataCriteria toReferrerDataCriteria(RefBookVersionEntity referrer,
                                                       List<Structure.Reference> references,
                                                       List<String> primaryValues) {
        List<Field> referenceFields = references.stream()
                .map(reference -> ConverterUtil.field(reference.getAttribute(), FieldType.REFERENCE))
                .collect(toList());
        Set<List<FieldSearchCriteria>> fieldSearchCriterias = toReferenceSearchCriterias(references, primaryValues);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                referrer.getStorageCode(), // Без учёта локализации
                referrer.getFromDate(), referrer.getToDate(),
                referenceFields, fieldSearchCriterias, null);
        dataCriteria.setPage(BaseDataCriteria.MIN_PAGE);
        dataCriteria.setSize(REF_BOOK_VERSION_DATA_PAGE_SIZE);

        return dataCriteria;
    }

    private Set<List<FieldSearchCriteria>> toReferenceSearchCriterias(List<Structure.Reference> references,
                                                                      List<String> primaryValues) {

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        references.forEach(reference -> {

            FieldSearchCriteria criteria = ConverterUtil.toFieldSearchCriteria(reference.getAttribute(),
                    FieldType.REFERENCE, SearchTypeEnum.EXACT, primaryValues);
            fieldSearchCriterias.add(singletonList(criteria));
        });

        return fieldSearchCriterias;
    }

    private void deleteReferrerConflicts(RefBookVersionEntity referrer, Collection<? extends RowValue> rowValues) {

        List<Long> refRecordIds = RowUtils.toSystemIds(rowValues);
        getConflictRepository().deleteByReferrerVersionIdAndRefRecordIdIn(referrer.getId(), refRecordIds);
    }

    private List<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                 RefBookVersionEntity entity,
                                                                 List<String> referenceCodes,
                                                                 List<String> primaryValues,
                                                                 Collection<? extends RowValue> rowValues) {
        if (isEmpty(rowValues))
            return emptyList();

        return rowValues.stream()
                .flatMap(rowValue ->
                        recalculateDataConflicts(referrer, entity, referenceCodes, primaryValues, rowValue)
                )
                .collect(toList());
    }

    private Stream<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                   RefBookVersionEntity entity,
                                                                   List<String> referenceCodes,
                                                                   List<String> primaryValues,
                                                                   RowValue rowValue) {
        return referenceCodes.stream()
                .filter(code -> {
                    String value = getReferenceValue(rowValue, code);
                    return value != null && primaryValues.contains(value);
                })
                .map(code ->
                        new RefBookConflictEntity(referrer, entity,
                                (Long) rowValue.getSystemId(), code, ConflictType.DELETED)
                );
    }

    private String getReferenceValue(RowValue rowValue, String code) {

        Serializable value = rowValue.getFieldValue(code).getValue();
        return value != null ? ((Reference) value).getValue() : null;
    }
}
