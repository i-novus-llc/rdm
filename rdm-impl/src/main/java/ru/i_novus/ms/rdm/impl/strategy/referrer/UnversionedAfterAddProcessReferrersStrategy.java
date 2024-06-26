package ru.i_novus.ms.rdm.impl.strategy.referrer;

import org.springframework.beans.factory.annotation.Autowired;
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
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.DataPageIterator;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedAfterAddProcessReferrersStrategy implements Strategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    public void apply(RefBookVersionEntity entity, List<RowValue> newRowValues) {

        if (isEmpty(newRowValues))
            return;

        final List<Structure.Attribute> primaries = entity.getStructure().getPrimaries();
        if (primaries.isEmpty())
            return;

        // Для поиска существующих конфликтов нужны сохранённые значения добавленных записей.
        final Collection<RowValue> addedRowValues = findAddedRowValues(entity, newRowValues, primaries);
        processReferrers(entity, primaries, addedRowValues);
    }

    public void processReferrers(RefBookVersionEntity entity,
                                 List<Structure.Attribute> primaries,
                                 Collection<RowValue> addedRowValues) {
        if (isEmpty(addedRowValues))
            return;

        final Map<String, RowValue> referredRowValues = RowUtils.toReferredRowValues(primaries, addedRowValues);
        if (isEmpty(referredRowValues))
            return;

        new ReferrerEntityIteratorProvider(versionRepository, entity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(referrer ->
                        processReferrer(referrer, entity, referredRowValues)
                )
        );
    }

    private Collection<RowValue> findAddedRowValues(RefBookVersionEntity entity,
                                                    List<RowValue> rowValues,
                                                    List<Structure.Attribute> primaries) {

        final StorageDataCriteria dataCriteria = toEntityDataCriteria(entity, rowValues, primaries);
        return searchDataService.getPagedData(dataCriteria).getCollection();
    }

    private StorageDataCriteria toEntityDataCriteria(RefBookVersionEntity entity,
                                                     List<RowValue> rowValues,
                                                     List<Structure.Attribute> primaries) {

        final Set<List<FieldSearchCriteria>> primarySearchCriterias = toPrimarySearchCriterias(rowValues, primaries);

        final StorageDataCriteria dataCriteria = new StorageDataCriteria(
                entity.getStorageCode(), // Без учёта локализации
                entity.getFromDate(), entity.getToDate(),
                ConverterUtil.fields(entity.getStructure()), primarySearchCriterias, null
        );
        dataCriteria.setPage(DataCriteria.FIRST_PAGE);
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
                        ConverterUtil.toFieldSearchCriteria(primary.getCode(), primary.getType(), SearchTypeEnum.EXACT,
                                singletonList(RowUtils.toSearchValue(primary, rowValue)))
                ).collect(toList());
    }

    /**
     * Обработка ссылочного справочника.
     *
     * @param referrer  сущность-версия, ссылающаяся на текущий справочник
     * @param entity    сущность-версия, на которую есть ссылки
     * @param rowValues набор добавляемых записей в entity
     */
    private void processReferrer(RefBookVersionEntity referrer,
                                 RefBookVersionEntity entity,
                                 Map<String, RowValue> rowValues) {

        final String refBookCode = entity.getRefBook().getCode();
        final List<Structure.Reference> references = referrer.getStructure().getRefCodeReferences(refBookCode);

        // storageCode - Без учёта локализации
        final ReferrerDataCriteria dataCriteria = new ReferrerDataCriteria(referrer, references,
                referrer.getStorageCode(), new ArrayList<>(rowValues.keySet()));
        final DataPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new DataPageIterator<>(searchDataService::getPagedData, dataCriteria);
        pageIterator.forEachRemaining(page ->

            // При наличии конфликта DELETED:
            // если запись восстановлена, то удалить конфликт,
            // иначе заменить тип конфликта на UPDATED.
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
        final List<Long> refRecordIds = RowUtils.toSystemIds(refRowValues);
        final String referenceCode = reference.getAttribute();
        final List<RefBookConflictEntity> conflicts =
                conflictRepository.findByReferrerVersionIdAndRefFieldCodeAndConflictTypeAndRefRecordIdIn(
                        referrer.getId(), referenceCode, ConflictType.DELETED, refRecordIds
                );
        if (isEmpty(conflicts))
            return;

        // Определить действия над конфликтами по результату сравнения отображаемых значений.
        final List<RefBookConflictEntity> toUpdate = new ArrayList<>(conflicts.size());
        final List<RefBookConflictEntity> toDelete = new ArrayList<>(conflicts.size());

        for (RefBookConflictEntity conflict : conflicts) {

            final Reference fieldReference =
                    RowUtils.getFieldReference(refRowValues, conflict.getRefRecordId(), referenceCode);
            final RowValue addedRowValue =
                    (fieldReference != null) ? addedRowValues.get(fieldReference.getValue()) : null;
            if (addedRowValue == null) continue;

            final String newDisplayValue = FieldValueUtils.toDisplayValue(
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
