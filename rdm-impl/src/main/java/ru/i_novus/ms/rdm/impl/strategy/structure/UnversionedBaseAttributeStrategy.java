package ru.i_novus.ms.rdm.impl.strategy.structure;

import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferredDataCriteria;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferrerDataCriteria;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.CollectionPageIterator;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.StructureUtils.hasAbsentPlaceholder;

@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedBaseAttributeStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    protected void processReferrers(RefBookVersionEntity entity) {

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
     * @param referrer  сущность-версия, ссылающаяся на текущий справочник
     * @param entity    сущность-версия, на которую есть ссылки
     * @param primaries первичные ключи текущего справочника
     */
    private void processReferrer(RefBookVersionEntity referrer, RefBookVersionEntity entity,
                                 List<Structure.Attribute> primaries) {

        String refBookCode = entity.getRefBook().getCode();
        List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);
        references.forEach(reference ->

                // Если displayExpression восстановлен,
                // то удалить конфликт DISPLAY_DAMAGED при его наличии,
                // иначе создать конфликт DISPLAY_DAMAGED при его отсутствии.
                recalculateDisplayDamagedConflicts(referrer, entity, reference)
        );

        // storageCode - Без учёта локализации
        ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references, referrer.getStorageCode(), null);
        dataCriteria.setFieldFilters(ConverterUtil.toNotNullSearchCriterias(references));

        CollectionPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new CollectionPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page ->

                // Если hash записи восстановлен,
                // то удалить конфликт ALTERED при его наличии,
                // иначе создать конфликт ALTERED при его отсутствии.
                recalculateAlteredConflicts(referrer, entity, primaries, references, page.getCollection())
        );
    }

    private void recalculateDisplayDamagedConflicts(RefBookVersionEntity referrer,
                                                    RefBookVersionEntity entity,
                                                    Structure.Reference reference) {

        // Найти существующие конфликты DISPLAY_DAMAGED для текущей ссылки.
        String referenceCode = reference.getAttribute();
        List<RefBookConflictEntity> conflicts =
                        conflictRepository.findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIsNull(
                                referrer.getId(), referenceCode, ConflictType.DISPLAY_DAMAGED
                        );

        // Определить и выполнить действия над конфликтами по результату
        // проверки выражения для вычисления отображаемого значения.
        boolean isError = hasAbsentPlaceholder(reference.getDisplayExpression(), entity.getStructure());
        if (isError && isEmpty(conflicts)) {

            // Ошибка в выражении ссылки:
            RefBookConflictEntity added = new RefBookConflictEntity(referrer, entity,
                    null, referenceCode, ConflictType.DISPLAY_DAMAGED);
            conflictRepository.save(added); // добавить конфликт

        } else if (!isError && !isEmpty(conflicts)) {

            // Восстановление выражения ссылки:
            conflictRepository.deleteAll(conflicts); // удалить конфликты
        }
    }

    private void recalculateAlteredConflicts(RefBookVersionEntity referrer,
                                             RefBookVersionEntity entity,
                                             List<Structure.Attribute> primaries,
                                             List<Structure.Reference> references,
                                             Collection<? extends RowValue> refRowValues) {
        references.forEach(reference ->
                recalculateAlteredConflicts(referrer, entity, primaries, reference, refRowValues)
        );
    }

    private void recalculateAlteredConflicts(RefBookVersionEntity referrer,
                                             RefBookVersionEntity entity,
                                             List<Structure.Attribute> primaries,
                                             Structure.Reference reference,
                                             Collection<? extends RowValue> refRowValues) {

        // Найти существующие конфликты ALTERED для текущей ссылки.
        String referenceCode = reference.getAttribute();
        List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);
        List<RefBookConflictEntity> conflicts =
                conflictRepository.findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIn(
                        referrer.getId(), referenceCode, ConflictType.ALTERED, refRecordIds
                );

        List<RefBookConflictEntity> toAdd = new ArrayList<>(refRowValues.size());
        List<RefBookConflictEntity> toDelete = new ArrayList<>(conflicts.size());

        Collection<RowValue> rowValues = findReferredRowValues(entity, primaries, referenceCode, refRowValues);
        if (isEmpty(rowValues))
            return;

        Map<String, RowValue> referredRowValues = RowUtils.toReferredRowValues(primaries, rowValues);

        for (RowValue refRowValue : refRowValues) {

            // Определить действия над конфликтами по результату сравнения hash-значений.
            boolean isRestored = isHashRestored(refRowValue, referenceCode, referredRowValues);

            Long refRecordId = (Long) refRowValue.getSystemId();
            List<RefBookConflictEntity> refConflicts = conflicts.stream()
                    .filter(conflict -> Objects.equals(conflict.getRefRecordId(), refRecordId))
                    .collect(toList());
            if (isRestored && !isEmpty(refConflicts)) {

                // Восстановление hash-значения в ссылке:
                toDelete.addAll(refConflicts);
                conflicts.removeAll(refConflicts);

            } else if (!isRestored && isEmpty(refConflicts)) {
                
                // Изменение hash-значения в ссылке:
                RefBookConflictEntity added = new RefBookConflictEntity(referrer, entity,
                        refRecordId, referenceCode, ConflictType.ALTERED);
                toAdd.add(added);
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

    private Collection<RowValue> findReferredRowValues(RefBookVersionEntity entity,
                                                       List<Structure.Attribute> primaries,
                                                       String referenceCode,
                                                       Collection<? extends RowValue> refRowValues) {

        List<String> referenceValues = RowUtils.getFieldReferenceValues(refRowValues, referenceCode);
        if (isEmpty(referenceValues))
            return emptyList();

        StorageDataCriteria dataCriteria = new ReferredDataCriteria(entity, primaries,
                entity.getStorageCode(), primaries, referenceValues);
        return searchDataService.getPagedData(dataCriteria).getCollection();
    }

    private boolean isHashRestored(RowValue refRowValue, String referenceCode,
                                   Map<String, RowValue> referredRowValues) {

        Reference fieldReference = RowUtils.getFieldReference(refRowValue, referenceCode);
        RowValue referredRowValue = (fieldReference != null) ? referredRowValues.get(fieldReference.getValue()) : null;
        return referredRowValue != null &&
                fieldReference.hashCode() == referredRowValue.hashCode();
    }
}
