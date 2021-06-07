package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferrerDataCriteria;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
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
public class UnversionedDeleteAllRowValuesStrategy extends DefaultDeleteAllRowValuesStrategy {

    private static final List<? extends Serializable> NOT_NULL_VALUES = List.of(0L);

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Override
    protected void before(RefBookVersionEntity entity) {

        super.before(entity);

        processReferrers(entity);
    }

    private void processReferrers(RefBookVersionEntity entity) {

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity)
                )
        );
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer сущность-версия, ссылающаяся на текущий справочник
     * @param entity   сущность-версия, на которую есть ссылки
     */
    private void processReferrer(RefBookVersionEntity referrer, RefBookVersionEntity entity) {

        // Удалить существующие конфликты для всех записей.
        deleteAllDataConflicts(referrer, entity);

        String refBookCode = entity.getRefBook().getCode();
        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);
        List<String> referenceCodes = references.stream().map(Structure.Reference::getAttribute).collect(toList());

        // storageCode - Без учёта локализации
        ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references, referrer.getStorageCode(), null);
        dataCriteria.setFieldFilters(toReferenceSearchCriterias(references));

        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page -> {

            // Если есть значение ссылки на один из systemIds, создать конфликт DELETED.
            List<RefBookConflictEntity> conflicts = recalculateDataConflicts(
                    referrer, referenceCodes, entity, page.getCollection()
            );
            if (!isEmpty(conflicts)) {
                getConflictRepository().saveAll(conflicts);
            }
        });
    }

    private void deleteAllDataConflicts(RefBookVersionEntity referrer, RefBookVersionEntity entity) {

        getConflictRepository().deleteByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdIsNotNull(
                referrer.getId(), entity.getId()
        );
    }

    /**
     * Преобразование ссылок на ненулевые значения первичных ключей в набор критериев поиска по полям-ссылкам.
     *
     * @param references ссылки
     * @return Набор критериев поиска по полям-ссылкам
     */
    private Set<List<FieldSearchCriteria>> toReferenceSearchCriterias(List<Structure.Reference> references) {

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        references.forEach(reference -> {

            FieldSearchCriteria criteria = ConverterUtil.toFieldSearchCriteria(reference.getAttribute(),
                    FieldType.REFERENCE, SearchTypeEnum.IS_NOT_NULL, NOT_NULL_VALUES);
            fieldSearchCriterias.add(singletonList(criteria));
        });

        return fieldSearchCriterias;
    }

    private List<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                 List<String> referenceCodes,
                                                                 RefBookVersionEntity entity,
                                                                 Collection<? extends RowValue> refRowValues) {
        if (isEmpty(refRowValues))
            return emptyList();

        return refRowValues.stream()
                .flatMap(rowValue ->
                        recalculateDataConflicts(referrer, referenceCodes, entity, rowValue)
                )
                .collect(toList());
    }

    private Stream<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                   List<String> referenceCodes,
                                                                   RefBookVersionEntity entity,
                                                                   RowValue refRowValue) {
        return referenceCodes.stream()
                .filter(code -> getReferenceValue(refRowValue, code) != null)
                .map(code ->
                        new RefBookConflictEntity(referrer, entity,
                                (Long) refRowValue.getSystemId(), code, ConflictType.DELETED)
                );
    }

    private String getReferenceValue(RowValue rowValue, String code) {

        Serializable value = rowValue.getFieldValue(code).getValue();
        return value != null ? ((Reference) value).getValue() : null;
    }
}
