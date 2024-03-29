package ru.i_novus.ms.rdm.impl.strategy.structure;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferredDataCriteria;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferrerDataCriteria;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.util.DataPageIterator;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.StructureUtils.hasAbsentPlaceholder;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedChangeStructureStrategy implements Strategy {

    private static final String COMPARE_OLD_STRUCTURE_PRIMARIES_NOT_FOUND_EXCEPTION_CODE = "compare.old.structure.primaries.not.found";
    private static final String COMPARE_NEW_STRUCTURE_PRIMARIES_NOT_FOUND_EXCEPTION_CODE = "compare.new.structure.primaries.not.found";
    private static final String COMPARE_STRUCTURES_PRIMARIES_NOT_MATCH_EXCEPTION_CODE = "compare.structures.primaries.not.match";

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private SearchDataService searchDataService;

    @Autowired
    private VersionValidation versionValidation;

    public boolean hasReferrerVersions(RefBookVersionEntity entity) {

        return versionValidation.hasReferrerVersions(entity.getRefBook().getCode());
    }

    /**
     * Проверка первичных ключей структур справочника на совпадение.
     * <p/>
     * См. CompareServiceImpl.validatePrimariesEquality.
     *
     * @param refBookCode  код справочника
     * @param oldStructure старая структура справочника
     * @param newStructure новая структура справочника
     */
    public void validatePrimariesEquality(String refBookCode, Structure oldStructure, Structure newStructure) {

        List<Structure.Attribute> oldPrimaries = oldStructure.getPrimaries();
        if (isEmpty(oldPrimaries))
            throw new UserException(new Message(COMPARE_OLD_STRUCTURE_PRIMARIES_NOT_FOUND_EXCEPTION_CODE, refBookCode));

        List<Structure.Attribute> newPrimaries = newStructure.getPrimaries();
        if (isEmpty(newPrimaries))
            throw new UserException(new Message(COMPARE_NEW_STRUCTURE_PRIMARIES_NOT_FOUND_EXCEPTION_CODE, refBookCode));

        if (!versionValidation.equalsPrimaries(oldPrimaries, newPrimaries))
            throw new UserException(new Message(COMPARE_STRUCTURES_PRIMARIES_NOT_MATCH_EXCEPTION_CODE, refBookCode));
    }

    /**
     * Обработка ссылочных справочников.
     *
     * @param entity сущность-версия, на которую есть ссылки
     */
    public void processReferrers(RefBookVersionEntity entity) {

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

        DataPageIterator<RowValue, StorageDataCriteria> pageIterator =
                new DataPageIterator<>(searchDataService::getPagedData, dataCriteria);
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

            Reference fieldReference = RowUtils.getFieldReference(refRowValue, referenceCode);
            if (fieldReference == null) continue;

            // Определить действия над конфликтами по результату сравнения hash-значений.
            boolean isRestored = isHashRestored(fieldReference, referredRowValues);

            Long refRecordId = (Long) refRowValue.getSystemId();
            List<RefBookConflictEntity> refConflicts = conflicts.stream()
                    .filter(conflict -> Objects.equals(conflict.getRefRecordId(), refRecordId))
                    .collect(toList());
            if (isRestored && !isEmpty(refConflicts)) {

                // Восстановление hash-значения в ссылке:
                toDelete.addAll(refConflicts);
                conflicts.removeAll(refConflicts);

            } else if (!isRestored && isEmpty(refConflicts) &&
                    !StringUtils.isEmpty(fieldReference.getValue())) {
                
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

    /**
     * Получение записей по значениям ссылки.
     *
     * @param entity        новая версия исходного справочника
     * @param primaries     первичные ключи исходного справочника
     * @param referenceCode код атрибута-ссылки
     * @param refRowValues  записи ссылочного справочника
     * @return Записи исходного справочника
     */
    private Collection<RowValue> findReferredRowValues(RefBookVersionEntity entity,
                                                       List<Structure.Attribute> primaries,
                                                       String referenceCode,
                                                       Collection<? extends RowValue> refRowValues) {

        List<String> referenceValues = RowUtils.getFieldReferenceValues(refRowValues, referenceCode);
        if (isEmpty(referenceValues))
            return emptyList();

        StorageDataCriteria dataCriteria = new ReferredDataCriteria(entity, primaries,
                entity.getStorageCode(), primaries, referenceValues); // Без учёта локализации
        return searchDataService.getPagedData(dataCriteria).getCollection();
    }

    private boolean isHashRestored(Reference fieldReference, Map<String, RowValue> referredRowValues) {

        if (StringUtils.isEmpty(fieldReference.getHash()))
            return false;

        RowValue referredRowValue = referredRowValues.get(fieldReference.getValue());
        return referredRowValue != null &&
                Objects.equals(fieldReference.getHash(), referredRowValue.getHash());
    }
}
