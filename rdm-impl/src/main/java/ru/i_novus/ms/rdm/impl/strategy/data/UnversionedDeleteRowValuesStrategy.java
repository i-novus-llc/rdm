package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
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
    protected void after(RefBookVersionEntity entity, List<Object> systemIds) {

        super.after(entity, systemIds);

        processReferrers(entity, systemIds);
    }

    private void processReferrers(RefBookVersionEntity entity, List<Object> systemIds) {

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, systemIds)
                )
        );
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer  сущность-версия, ссылающаяся на текущий справочник
     * @param entity    сущность-версия, на которую есть ссылки
     * @param systemIds список системных идентификаторов записей
     */
    private void processReferrer(RefBookVersionEntity referrer,
                                 RefBookVersionEntity entity, List<Object> systemIds) {

        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(entity.getRefBook().getCode());
        Set<List<FieldSearchCriteria>> fieldSearchCriterias = toFieldSearchCriterias(references, systemIds);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                referrer.getStorageCode(), // Без учёта локализации
                referrer.getFromDate(), referrer.getToDate(),
                toFields(references), fieldSearchCriterias, null);
        dataCriteria.setPage(BaseDataCriteria.MIN_PAGE);
        dataCriteria.setSize(REF_BOOK_VERSION_DATA_PAGE_SIZE);

        List<String> referenceCodes = references.stream().map(Structure.Reference::getAttribute).collect(toList());
        List<String> referenceValues = systemIds.stream().map(id -> ((Long) id).toString()).collect(toList());

        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page -> {

            // Удалить существующие конфликты для найденных записей:
            deleteReferrerConflicts(referrer, page.getCollection());

            // Если есть значение ссылки на один из systemIds, создать конфликт DELETED:
            List<RefBookConflictEntity> entities = recalculateDataConflicts(referrer, entity,
                    referenceCodes, referenceValues, page.getCollection());
            if (!isEmpty(entities)) {
                getConflictRepository().saveAll(entities);
            }
        });
    }

    private void deleteReferrerConflicts(RefBookVersionEntity referrer, Collection<? extends RowValue> rowValues) {

        List<Long> refRecordIds = RowUtils.toSystemIds(rowValues);
        getConflictRepository().deleteByReferrerVersionIdAndRefRecordIdIn(referrer.getId(), refRecordIds);
    }

    private Set<List<FieldSearchCriteria>> toFieldSearchCriterias(List<Structure.Reference> references,
                                                                  List<Object> systemIds) {

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        references.forEach(reference -> {

            FieldSearchCriteria criteria = ConverterUtil.toFieldSearchCriteria(
                    reference.getAttribute(), FieldType.REFERENCE,
                    SearchTypeEnum.EXACT, RowUtils.toLongSystemIds(systemIds)
            );
            fieldSearchCriterias.add(singletonList(criteria));
        });

        return fieldSearchCriterias;
    }

    private List<Field> toFields(List<Structure.Reference> references) {

        return references.stream()
                .map(reference -> ConverterUtil.field(reference.getAttribute(), FieldType.REFERENCE))
                .collect(toList());
    }

    private List<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                 RefBookVersionEntity entity,
                                                                 List<String> referenceCodes,
                                                                 List<String> referenceValues,
                                                                 Collection<? extends RowValue> rowValues) {
        if (isEmpty(rowValues))
            return emptyList();

        return rowValues.stream()
                .flatMap(rowValue ->
                        recalculateDataConflicts(referrer, entity, referenceCodes, referenceValues, rowValue)
                )
                .collect(toList());
    }

    private Stream<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                   RefBookVersionEntity entity,
                                                                   List<String> referenceCodes,
                                                                   List<String> referenceValues,
                                                                   RowValue rowValue) {
        return referenceCodes.stream()
                .filter(code -> {
                    String value = getReferenceValue(rowValue, code);
                    return value != null && referenceValues.contains(value);
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
