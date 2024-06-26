package ru.i_novus.ms.rdm.impl.strategy.referrer;

import org.springframework.beans.factory.annotation.Autowired;
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
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.DataPageIterator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedBeforeDeleteProcessReferrersStrategy implements Strategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    public void apply(RefBookVersionEntity entity, List<Object> systemIds) {

        if (isEmpty(systemIds))
            return;

        final List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        if (primaries.isEmpty())
            return;

        // Для поиска записей-ссылок нужны не systemId, а строковые значения первичных ключей.
        final Collection<RowValue> deletedRowValues = findDeletedRowValues(entity, systemIds, primaries);
        processReferrers(entity, primaries, deletedRowValues);
    }

    private Collection<RowValue> findDeletedRowValues(RefBookVersionEntity entity,
                                                      List<Object> systemIds,
                                                      List<Structure.Attribute> primaries) {

        final StorageDataCriteria dataCriteria = toEntityDataCriteria(entity, systemIds, primaries);
        return searchDataService.getPagedData(dataCriteria).getCollection();
    }

    private StorageDataCriteria toEntityDataCriteria(RefBookVersionEntity entity,
                                                     List<Object> systemIds,
                                                     List<Structure.Attribute> primaries) {
        final List<Field> primaryFields = primaries.stream()
                .map(primary -> ConverterUtil.field(primary.getCode(), primary.getType()))
                .collect(toList());

        final StorageDataCriteria dataCriteria = new StorageDataCriteria(
                entity.getStorageCode(), // Без учёта локализации
                entity.getFromDate(), entity.getToDate(),
                primaryFields);
        dataCriteria.setSystemIds(RowUtils.toLongSystemIds(systemIds));

        dataCriteria.setPage(DataCriteria.FIRST_PAGE);
        dataCriteria.setSize(systemIds.size());

        return dataCriteria;
    }

    public void processReferrers(RefBookVersionEntity entity,
                                 List<Structure.Attribute> primaries,
                                 Collection<RowValue> deletedRowValues) {
        if (isEmpty(deletedRowValues))
            return;

        final List<String> primaryValues = RowUtils.toReferenceValues(primaries, deletedRowValues);
        if (isEmpty(primaryValues))
            return;

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, primaryValues)
                )
        );
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer      сущность-версия, ссылающаяся на текущий справочник
     * @param entity        сущность-версия, на которую есть ссылки
     * @param primaryValues значения первичных ключей записей
     */
    private void processReferrer(RefBookVersionEntity referrer,
                                 RefBookVersionEntity entity,
                                 List<String> primaryValues) {

        final String refBookCode = entity.getRefBook().getCode();
        final List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);
        final List<String> referenceCodes = references.stream().map(Structure.Reference::getAttribute).collect(toList());

        // storageCode - Без учёта локализации
        final ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references, referrer.getStorageCode(), primaryValues);
        final DataPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new DataPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page -> {

            // Удалить существующие конфликты для найденных записей.
            deleteDataConflicts(referrer, entity, page.getCollection());

            // Если есть значение ссылки на один из systemIds, создать конфликт DELETED.
            final List<RefBookConflictEntity> conflicts = recalculateDataConflicts(
                    referrer, entity, primaryValues, referenceCodes, page.getCollection()
            );
            if (!isEmpty(conflicts)) {
                conflictRepository.saveAll(conflicts);
            }
        });
    }

    private void deleteDataConflicts(RefBookVersionEntity referrer,
                                     RefBookVersionEntity entity,
                                     Collection<? extends RowValue> rowValues) {

        final List<Long> refRecordIds = RowUtils.toSystemIds(rowValues);
        conflictRepository.deleteByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdIn(
                referrer.getId(), entity.getId(), refRecordIds
        );
    }

    private List<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                 RefBookVersionEntity entity,
                                                                 List<String> primaryValues,
                                                                 List<String> referenceCodes,
                                                                 Collection<? extends RowValue> refRowValues) {
        if (isEmpty(refRowValues))
            return emptyList();

        return refRowValues.stream()
                .flatMap(rowValue ->
                        recalculateDataConflicts(referrer, entity, primaryValues, referenceCodes, rowValue)
                )
                .collect(toList());
    }

    private Stream<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                   RefBookVersionEntity entity,
                                                                   List<String> primaryValues,
                                                                   List<String> referenceCodes,
                                                                   RowValue refRowValue) {
        return referenceCodes.stream()
                .filter(code -> {
                    final String value = RowUtils.getFieldReferenceValue(refRowValue, code);
                    return value != null && primaryValues.contains(value);
                })
                .map(code ->
                        new RefBookConflictEntity(referrer, entity,
                                (Long) refRowValue.getSystemId(), code, ConflictType.DELETED)
                );
    }
}
