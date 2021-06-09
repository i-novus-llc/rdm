package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferrerDataCriteria;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.CollectionPageIterator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedDeleteAllRowValuesStrategy implements DeleteAllRowValuesStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Autowired
    @Qualifier("defaultDeleteAllRowValuesStrategy")
    private DeleteAllRowValuesStrategy deleteAllRowValuesStrategy;

    @Override
    public void deleteAll(RefBookVersionEntity entity) {

        processReferrers(entity);

        deleteAllRowValuesStrategy.deleteAll(entity);
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
        dataCriteria.setFieldFilters(ConverterUtil.toNotNullSearchCriterias(references));

        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page -> {

            // Если есть значение ссылки на один из systemIds, создать конфликт DELETED.
            List<RefBookConflictEntity> conflicts = recalculateDataConflicts(
                    referrer, referenceCodes, entity, page.getCollection()
            );
            if (!isEmpty(conflicts)) {
                conflictRepository.saveAll(conflicts);
            }
        });
    }

    private void deleteAllDataConflicts(RefBookVersionEntity referrer, RefBookVersionEntity entity) {

        conflictRepository.deleteByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdIsNotNull(
                referrer.getId(), entity.getId()
        );
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
                .filter(code -> RowUtils.getFieldReferenceValue(refRowValue, code) != null)
                .map(code ->
                        new RefBookConflictEntity(referrer, entity,
                                (Long) refRowValue.getSystemId(), code, ConflictType.DELETED)
                );
    }
}
