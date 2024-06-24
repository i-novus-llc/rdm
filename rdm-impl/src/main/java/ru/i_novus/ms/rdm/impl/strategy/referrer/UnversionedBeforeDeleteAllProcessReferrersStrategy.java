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
public class UnversionedBeforeDeleteAllProcessReferrersStrategy implements Strategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    public void apply(RefBookVersionEntity entity) {

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

        // Удалить существующие конфликты данных для всех записей.
        deleteAllDataConflicts(referrer, entity);

        final String refBookCode = entity.getRefBook().getCode();
        final List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);
        final List<String> referenceCodes = references.stream().map(Structure.Reference::getAttribute).collect(toList());

        // storageCode - Без учёта локализации
        final ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references, referrer.getStorageCode(), null);
        dataCriteria.setFieldFilters(ConverterUtil.toNotNullSearchCriterias(references));

        final DataPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new DataPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page -> {

            // Если есть значение ссылки на один из systemIds, создать конфликт DELETED.
            final List<RefBookConflictEntity> conflicts = recalculateDataConflicts(
                    referrer, entity, referenceCodes, page.getCollection()
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
                                                                 RefBookVersionEntity entity,
                                                                 List<String> referenceCodes,
                                                                 Collection<? extends RowValue> refRowValues) {
        if (isEmpty(refRowValues))
            return emptyList();

        return refRowValues.stream()
                .flatMap(rowValue ->
                        recalculateDataConflicts(referrer, entity, referenceCodes, rowValue)
                )
                .collect(toList());
    }

    private Stream<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity referrer,
                                                                   RefBookVersionEntity entity,
                                                                   List<String> referenceCodes,
                                                                   RowValue refRowValue) {
        return referenceCodes.stream()
                .filter(code -> RowUtils.getFieldReferenceValue(refRowValue, code) != null)
                .map(code ->
                        new RefBookConflictEntity(referrer, entity,
                                (Long) refRowValue.getSystemId(), code, ConflictType.DELETED)
                );
    }
}
