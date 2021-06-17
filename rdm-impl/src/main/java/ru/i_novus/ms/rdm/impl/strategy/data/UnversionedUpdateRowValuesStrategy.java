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
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.CollectionPageIterator;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedUpdateRowValuesStrategy implements UpdateRowValuesStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Autowired
    @Qualifier("defaultUpdateRowValuesStrategy")
    private UpdateRowValuesStrategy updateRowValuesStrategy;

    @Autowired
    private UnversionedAddRowValuesStrategy unversionedAddRowValuesStrategy;

    @Autowired
    private UnversionedDeleteRowValuesStrategy unversionedDeleteRowValuesStrategy;

    @Override
    public void update(RefBookVersionEntity entity, List<RowValue> oldRowValues, List<RowValue> newRowValues) {

        updateRowValuesStrategy.update(entity, oldRowValues, newRowValues);

        processReferrers(entity, oldRowValues, newRowValues);
    }

    private void processReferrers(RefBookVersionEntity entity,
                                  List<RowValue> oldRowValues, List<RowValue> newRowValues) {

        List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        if (primaries.isEmpty())
            return;

        Map<String, RowValue> oldReferredRowValues = RowUtils.toReferredRowValues(primaries, oldRowValues);
        Map<String, RowValue> newReferredRowValues = RowUtils.toReferredRowValues(primaries, newRowValues);

        // Обработка записей, в которых нет изменения первичных ключей.
        Map<String, RowValue> updatedRowValues = oldReferredRowValues.entrySet().stream()
                .filter(entry -> {
                    RowValue newRowValue = newReferredRowValues.get(entry.getKey());
                    return newRowValue != null &&
                            Objects.equals(entry.getValue().getSystemId(), newRowValue.getSystemId());
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        processReferrers(entity, primaries, updatedRowValues, newReferredRowValues);

        // Обработка записей, в которых есть изменения первичных ключей.
        List<RowValue> deletedRowValues = oldReferredRowValues.entrySet().stream()
                .filter(entry -> {
                    RowValue newRowValue = newReferredRowValues.get(entry.getKey());
                    return newRowValue == null ||
                            !Objects.equals(entry.getValue().getSystemId(), newRowValue.getSystemId());
                })
                .map(Map.Entry::getValue)
                .collect(toList());
        unversionedDeleteRowValuesStrategy.processReferrers(entity, primaries, deletedRowValues);

        List<RowValue> addedRowValues = newReferredRowValues.entrySet().stream()
                .filter(entry -> {
                    RowValue oldRowValue = oldReferredRowValues.get(entry.getKey());
                    return oldRowValue == null ||
                            !Objects.equals(entry.getValue().getSystemId(), oldRowValue.getSystemId());
                })
                .map(Map.Entry::getValue)
                .collect(toList());
        unversionedAddRowValuesStrategy.processReferrers(entity, primaries, addedRowValues);
    }

    private void processReferrers(RefBookVersionEntity entity, List<Structure.Attribute> primaries,
                                  Map<String, RowValue> oldRowValues, Map<String, RowValue> newRowValues) {

        if (isEmpty(oldRowValues) || isEmpty(newRowValues))
            return;

        List<String> primaryValues = RowUtils.toReferenceValues(primaries, oldRowValues.values());
        if (isEmpty(primaryValues))
            return;

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, primaryValues, oldRowValues, newRowValues)
                )
        );
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer      сущность-версия, ссылающаяся на текущий справочник
     * @param entity        сущность-версия, на которую есть ссылки
     * @param primaryValues значения первичных ключей старых записей
     * @param oldRowValues  набор старых записей в entity
     * @param newRowValues  набор новых записей в entity
     */
    private void processReferrer(RefBookVersionEntity referrer, RefBookVersionEntity entity, List<String> primaryValues,
                                 Map<String, RowValue> oldRowValues, Map<String, RowValue> newRowValues) {

        String refBookCode = entity.getRefBook().getCode();
        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);

        // storageCode - Без учёта локализации
        ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references, referrer.getStorageCode(), primaryValues);
        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page ->

            // Если отображаемое значение восстановилось - удалить конфликт UPDATED при его наличии,
            // иначе - создать конфликт UPDATED при его отсутствии.
            recalculateDataConflicts(referrer, entity, oldRowValues, newRowValues, references, page.getCollection())
        );
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer, RefBookVersionEntity entity,
                                          Map<String, RowValue> oldRowValues, Map<String, RowValue> newRowValues,
                                          List<Structure.Reference> references, Collection<? extends RowValue> refRowValues) {
        references.forEach(reference ->
                recalculateDataConflicts(referrer, entity, oldRowValues, newRowValues, reference, refRowValues)
        );
    }

    private void recalculateDataConflicts(RefBookVersionEntity referrer, RefBookVersionEntity entity,
                                          Map<String, RowValue> oldRowValues, Map<String, RowValue> newRowValues,
                                          Structure.Reference reference, Collection<? extends RowValue> refRowValues) {

        // Найти существующие конфликты UPDATED для текущей ссылки.
        List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);
        String referenceCode = reference.getAttribute();
        List<RefBookConflictEntity> conflicts =
                conflictRepository.findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIn(
                        referrer.getId(), referenceCode, ConflictType.UPDATED, refRecordIds
                );

        List<RefBookConflictEntity> toAdd = new ArrayList<>(conflicts.size());
        List<RefBookConflictEntity> toDelete = new ArrayList<>(conflicts.size());

        for (RowValue refRowValue : refRowValues) {

            // Определить действия над конфликтами по результату
            // сравнения первичных ключей и отображаемых значений.
            RefBookConflictEntity conflict = conflicts.stream()
                    .filter(c -> Objects.equals(c.getRefRecordId(), refRowValue.getSystemId()))
                    .findFirst().orElse(null);

            Reference fieldReference = RowUtils.getFieldReference(refRowValue, referenceCode);
            if (fieldReference == null) continue;

            RowValue oldRowValue = oldRowValues.get(fieldReference.getValue());
            RowValue newRowValue = RowUtils.getBySystemId(newRowValues.values(), oldRowValue.getSystemId());

            String newDisplayValue = FieldValueUtils.toDisplayValue(
                    reference.getDisplayExpression(), newRowValue, null);
            boolean isEqual = Objects.equals(fieldReference.getDisplayValue(), newDisplayValue);

            if (isEqual && conflict != null) {

                // Восстановление записи для ссылки:
                toDelete.add(conflict); // удалить конфликт

            } else
            if (!isEqual && conflict == null) {
                // Изменение исходной записи:
                RefBookConflictEntity changed = new RefBookConflictEntity(referrer, entity,
                        (Long) refRowValue.getSystemId(), referenceCode, ConflictType.UPDATED);
                toAdd.add(changed); // создать UPDATED-конфликт
            }
        }

        // Выполнить действия над конфликтами.
        if (!isEmpty(toAdd)) {
            conflictRepository.saveAll(toAdd);
        }

        if (!isEmpty(toDelete)) {
            conflictRepository.deleteAll(toDelete);
        }
    }
}
