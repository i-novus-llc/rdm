package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.conflict.DeleteRefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.field.ReferenceFilterValue;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.ConflictUtils;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.model.refdata.ReferredDataCriteria;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookConflictQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.ComparableUtils.*;
import static ru.i_novus.ms.rdm.api.util.ConflictUtils.conflictTypeToDiffStatus;
import static ru.i_novus.ms.rdm.api.util.ConflictUtils.diffStatusToConflictType;
import static ru.i_novus.ms.rdm.api.util.FieldValueUtils.*;
import static ru.i_novus.ms.rdm.api.util.StructureUtils.containsAnyPlaceholder;
import static ru.i_novus.ms.rdm.api.util.StructureUtils.hasAbsentPlaceholder;
import static ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE;

@Primary
@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class ConflictServiceImpl implements ConflictService {

    private static final int REF_BOOK_VERSION_PAGE_SIZE = 100;
    static final int REF_BOOK_VERSION_DATA_PAGE_SIZE = 100;

    private static final List<DiffStatusEnum> CALCULATING_DIFF_STATUSES = asList(DiffStatusEnum.DELETED, DiffStatusEnum.UPDATED);
    private static final List<ConflictType> ALTERED_RECALCULATING_CONFLICT_TYPES = singletonList(ConflictType.DELETED);

    private final RefBookVersionRepository versionRepository;
    private final RefBookConflictRepository conflictRepository;
    private final RefBookConflictQueryProvider conflictQueryProvider;

    private final CompareService compareService;
    private final SearchDataService searchDataService;

    private final VersionService versionService;
    private final VersionValidation versionValidation;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public ConflictServiceImpl(RefBookVersionRepository versionRepository,
                               RefBookConflictRepository conflictRepository,
                               RefBookConflictQueryProvider conflictQueryProvider,
                               CompareService compareService,
                               SearchDataService searchDataService,
                               VersionService versionService,
                               VersionValidation versionValidation) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;
        this.conflictQueryProvider = conflictQueryProvider;

        this.compareService = compareService;
        this.searchDataService = searchDataService;

        this.versionService = versionService;
        this.versionValidation = versionValidation;
    }

    /**
     * Поиск конфликтов по критерию.
     *
     * @param criteria критерий поиска
     * @return Страница конфликтов
     */
    @Override
    public Page<RefBookConflict> search(RefBookConflictCriteria criteria) {
        Page<RefBookConflictEntity> entities = conflictQueryProvider.search(criteria);
        return entities.map(this::refBookConflictModel);
    }

    @Override
    public Long countConflictedRowIds(RefBookConflictCriteria criteria) {
        return conflictQueryProvider.countConflictedRowIds(criteria);
    }

    @Override
    public Page<Long> searchConflictedRowIds(RefBookConflictCriteria criteria) {
        return conflictQueryProvider.searchConflictedRowIds(criteria);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        conflictRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void delete(DeleteRefBookConflictCriteria criteria) {
        conflictQueryProvider.delete(criteria);
    }

    /**
     * Получение идентификаторов конфликтов для версии, которая ссылается,
     * с любыми справочниками по указанным системным идентификаторам записей.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     * @param refRecordIds      список системных идентификаторов записей версии
     * @return Список конфликтных идентификаторов
     */
    @Override
    public List<Long> getReferrerConflictedIds(Integer referrerVersionId, List<Long> refRecordIds) {

        versionValidation.validateVersionExists(referrerVersionId);

        if (isEmpty(refRecordIds))
            return emptyList();

        return conflictRepository.findReferrerConflictedIds(referrerVersionId, refRecordIds, RefBookVersionStatus.PUBLISHED);
    }

    /**
     * Получение справочников, имеющих конфликты
     * с неопубликованной версией проверяемого справочника.
     *
     * @param versionId    идентификатор неопубликованной версии справочника
     * @param conflictType тип конфликта
     * @return Список справочников
     */
    @Override
    @Transactional(readOnly = true)
    public List<RefBookVersion> getConflictingReferrers(Integer versionId, ConflictType conflictType) {

        versionValidation.validateVersionExists(versionId);

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        String refBookCode = versionEntity.getRefBook().getCode();
        RefBookVersionEntity lastPublishedEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED);
        if (lastPublishedEntity == null)
            return emptyList();

        Integer lastPublishedId = lastPublishedEntity.getId();
        List<RefBookVersionEntity> conflictedReferrers = new ArrayList<>(REF_BOOK_VERSION_PAGE_SIZE);
        new ReferrerEntityIteratorProvider(versionRepository, refBookCode, RefBookSourceType.LAST_VERSION)
                .iterate().forEachRemaining(referrers -> {
                    List<RefBookVersionEntity> list = referrers.getContent().stream()
                            .filter(referrer -> checkConflicts(referrer.getId(), lastPublishedId, versionId, conflictType))
                            .collect(toList());
                    conflictedReferrers.addAll(list);
                });
        return conflictedReferrers.stream().map(ModelGenerator::versionModel).collect(toList());
    }

    /**
     * Проверка на наличие конфликта справочников при наличии ссылочных атрибутов.
     *
     * @param refFromId  идентификатор версии, которая ссылается
     * @param oldRefToId идентификатор старой версии, на которую ссылаются
     * @param newRefToId идентификатор новой версии, на которую будут ссылаться
     * @return Наличие конфликтов для версии, которая ссылается
     */
    @Override
    @Transactional(readOnly = true)
    public Boolean checkConflicts(Integer refFromId, Integer oldRefToId, Integer newRefToId, ConflictType conflictType) {

        RefBookVersionEntity refFromEntity = findVersionOrThrow(refFromId);
        RefBookVersionEntity oldRefToEntity = findVersionOrThrow(oldRefToId);

        if (ConflictType.ALTERED.equals(conflictType) || ConflictType.DISPLAY_DAMAGED.equals(conflictType)) {
            StructureDiff structureDiff = compareService.compareStructures(oldRefToId, newRefToId);

            if (ConflictType.ALTERED.equals(conflictType))
                return isRefBookAltered(structureDiff);

            List<Structure.Reference> refFromReferences = refFromEntity.getStructure().getRefCodeReferences(oldRefToEntity.getRefBook().getCode());
            return isDisplayDamagedConflict(refFromReferences, structureDiff);
        }

        DiffStatusEnum diffStatus = conflictTypeToDiffStatus(conflictType);

        CompareDataCriteria compareCriteria = new CompareDataCriteria(oldRefToId, newRefToId);
        compareCriteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(
                pageCriteria -> compareService.compareData(pageCriteria).getRows(),
                compareCriteria, true);
        while (pageIterator.hasNext()) {
            Page<? extends DiffRowValue> page = pageIterator.next();

            Boolean checked = checkDataDiffConflicts(refFromEntity, oldRefToEntity, getDataDiffContent(page, false), diffStatus);
            if (Boolean.TRUE.equals(checked))
                return true;
        }

        return false;
    }

    /**
     * Проверка на наличие конфликтов справочников по diff-записям.
     *
     * @param refFromEntity версия, которая ссылается
     * @param refToEntity   версия, на которую ссылаются
     * @param diffRowValues diff-записи
     * @param diffStatus    статус diff-записи
     * @return Наличие конфликтов для версии, которая ссылается
     * @see #calculateDataDiffConflicts
     */
    private boolean checkDataDiffConflicts(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                           List<DiffRowValue> diffRowValues, DiffStatusEnum diffStatus) {

        List<Structure.Attribute> refToPrimaries = refToEntity.getStructure().getPrimaries();
        List<Structure.Attribute> refFromAttributes = refFromEntity.getStructure().getRefCodeAttributes(refToEntity.getRefBook().getCode());
        List<RefBookRowValue> refFromRowValues = getConflictedRowContent(refFromEntity, diffRowValues, refToPrimaries, refFromAttributes);

        return refFromAttributes.stream()
                .anyMatch(refFromAttribute ->
                        diffRowValues.stream()
                                .filter(diffRowValue ->
                                        diffStatus.equals(diffRowValue.getStatus()))
                                .anyMatch(diffRowValue ->
                                        findRefBookRowValue(refToPrimaries, refFromAttribute, diffRowValue, refFromRowValues) != null
                                )
                );
    }

    /**
     * Обнаружение конфликтов при смене версий.
     *
     * @param oldVersionId идентификатор старой версии справочника
     * @param newVersionId идентификатор новой версии справочника
     */
    @Override
    @Transactional
    public void discoverConflicts(Integer oldVersionId, Integer newVersionId) {

        RefBookVersionEntity oldRefToEntity = findVersionOrThrow(oldVersionId);
        RefBookVersionEntity newRefToEntity = findVersionOrThrow(newVersionId);
        StructureDiff structureDiff = compareService.compareStructures(oldVersionId, newVersionId);

        new ReferrerEntityIteratorProvider(versionRepository, oldRefToEntity.getRefBook().getCode(), RefBookSourceType.ALL)
                .iterate().forEachRemaining(referrers ->
                referrers.getContent().forEach(refFromEntity ->
                        discoverConflicts(refFromEntity, oldRefToEntity, newRefToEntity, structureDiff)
                )
        );
    }

    /**
     * Копирование конфликтов при смене версий справочника без изменений.
     *
     * @param oldVersionId идентификатор старой версии справочника
     * @param newVersionId идентификатор новой версии справочника
     */
    @Override
    @Transactional
    // NB: for ApplicationTest only.
    public void copyConflicts(Integer oldVersionId, Integer newVersionId) {

        if (newVersionId.equals(oldVersionId))
            return;

        versionValidation.validateVersionExists(oldVersionId);
        versionValidation.validateVersionExists(newVersionId);

        conflictRepository.copyByReferrerVersion(oldVersionId, newVersionId);
    }

    private RefBookConflict refBookConflictModel(RefBookConflictEntity entity) {
        if (entity == null)
            return null;

        return new RefBookConflict(entity.getReferrerVersion().getId(), entity.getPublishedVersion().getId(),
                entity.getRefRecordId(), entity.getRefFieldCode(), entity.getConflictType(), entity.getCreationDate());
    }

    /**
     * Получение записей данных версии справочника для diff-записей.
     *
     * @param refFromEntity     версия справочника, который ссылается
     * @param diffRowValues     diff-записи
     * @param refToPrimaries    первичные ключи справочника, на который ссылаются
     * @param refFromAttributes ссылочные атрибуты версии, которая ссылается
     * @return Список всех записей
     */
    private List<RefBookRowValue> getConflictedRowContent(RefBookVersionEntity refFromEntity, List<DiffRowValue> diffRowValues,
                                                          List<Structure.Attribute> refToPrimaries, List<Structure.Attribute> refFromAttributes) {
        Set<List<FieldSearchCriteria>> filters = createDiffRowValuesFilters(diffRowValues, refToPrimaries, refFromAttributes);
        StorageDataCriteria criteria = new StorageDataCriteria(
                refFromEntity.getStorageCode(), // Без учёта локализации
                refFromEntity.getFromDate(), refFromEntity.getToDate(),
                ConverterUtil.fields(refFromEntity.getStructure()), filters, null);
        criteria.makeUnpaged(); // NB: Get all required rows since filters.size() <= REF_BOOK_DIFF_CONFLICT_PAGE_SIZE.

        DataPage<RowValue> pagedData = searchDataService.getPagedData(criteria);
        if (pagedData.getCollection() == null)
            return emptyList();

        return pagedData.getCollection().stream()
                .map(rowValue -> new RefBookRowValue((LongRowValue) rowValue, refFromEntity.getId()))
                .collect(toList());
    }

    /**
     * Получение записей по системным идентификаторам.
     *
     * @param versionId идентификатор версии
     * @param systemIds системные идентификаторы записей
     */
    private List<RefBookRowValue> getSystemRowValues(Integer versionId, List<Long> systemIds) {
        if (versionId == null || isEmpty(systemIds))
            return emptyList();

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);
        criteria.setRowSystemIds(systemIds);

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return (rowValues != null && !isEmpty(rowValues.getContent())) ? rowValues.getContent() : emptyList();
    }

    /**
     * Сравнение записей данных версий справочников для значений ссылочных полей.
     *
     * @param oldVersionId идентификатор старой версии
     * @param newVersionId идентификатор новой версии
     * @param filterValues значения ссылочных полей
     * @return Список различий
     */
    private List<DiffRowValue> toDiffRowValues(Integer oldVersionId, Integer newVersionId, List<ReferenceFilterValue> filterValues) {

        CompareDataCriteria criteria = new CompareDataCriteria(oldVersionId, newVersionId);
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);
        criteria.setPrimaryAttributesFilters(toAttributeFilters(filterValues));

        return compareService.compareData(criteria).getRows().getContent();
    }

    /**
     * Получение diff-записей данных версий справочников для конфликтов.
     *
     * @param diffRowValues список всех различий
     * @param isAltered     наличие изменения структуры
     * @return Список различий
     */
    private List<DiffRowValue> getDataDiffContent(Page<? extends DiffRowValue> diffRowValues, boolean isAltered) {
        return diffRowValues.getContent().stream()
                .filter(diffRowValue -> isAltered
                        ? DiffStatusEnum.DELETED.equals(diffRowValue.getStatus())
                        : CALCULATING_DIFF_STATUSES.contains(diffRowValue.getStatus()))
                .collect(toList());
    }

    /**
     * Получение ссылочных значений для фильтрации.
     *
     * @param refFromEntity    версия справочника, которая ссылается
     * @param refToEntity      версия справочника, на которую ссылались
     * @param conflicts        список конфликтов
     * @param refFromRowValues список записей версии справочника, которая ссылается
     * @return Список ссылочных значений
     */
    private List<ReferenceFilterValue> toFilterValues(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                                      List<? extends RefBookConflictEntity> conflicts, List<RefBookRowValue> refFromRowValues) {
        return conflicts.stream()
                .filter(conflict -> Objects.nonNull(conflict.getRefRecordId()))
                .map(conflict -> {
                    RefBookRowValue refFromRowValue = findConflictedValue(refFromRowValues, conflict);
                    if (refFromRowValue == null)
                        return null;

                    Structure.Reference refFromReference = refFromEntity.getStructure().getReference(conflict.getRefFieldCode());
                    Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(refToEntity.getStructure());
                    ReferenceFieldValue fieldValue = (ReferenceFieldValue) (refFromRowValue.getFieldValue(conflict.getRefFieldCode()));
                    return new ReferenceFilterValue(refToAttribute, fieldValue);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private RefBookRowValue findConflictedValue(List<RefBookRowValue> refFromRowValues, RefBookConflictEntity conflict) {

        return refFromRowValues.stream()
                .filter(rowValue -> rowValue.getSystemId().equals(conflict.getRefRecordId()))
                .findFirst().orElse(null);
    }

    /**
     * Создание фильтров для получения записей данных версии справочника по первичным ключам.
     *
     * @param diffRowValues     diff-записи
     * @param refToPrimaries    первичные ключи справочника, на который ссылаются
     * @param refFromAttributes ссылочные атрибуты версии, которая ссылается
     * @return Множество списков фильтров
     */
    private Set<List<FieldSearchCriteria>> createDiffRowValuesFilters(List<DiffRowValue> diffRowValues,
                                                                      List<Structure.Attribute> refToPrimaries,
                                                                      List<Structure.Attribute> refFromAttributes) {
        return diffRowValues.stream()
                .flatMap(diff -> {
                    // На данный момент первичным ключом может быть только одно поле.
                    // Ссылка на значение составного ключа невозможна.
                    DiffFieldValue diffFieldValue = diff.getDiffFieldValue(refToPrimaries.get(0).getCode());
                    Serializable value = getDiffFieldValue(diffFieldValue, diff.getStatus());

                    return refFromAttributes.stream()
                            .map(attribute -> singletonList(toDiffFieldSearchCriteria(attribute, value)));
                }).collect(toSet());
    }

    private FieldSearchCriteria toDiffFieldSearchCriteria(Structure.Attribute attribute, Serializable value) {

        SearchTypeEnum searchType = (attribute.getType() != FieldType.REFERENCE) ? SearchTypeEnum.EXACT : SearchTypeEnum.REFERENCE;
        return new FieldSearchCriteria(ConverterUtil.field(attribute), searchType, singletonList(value));
    }

    /**
     * Обнаружение конфликтов при смене версий.
     *
     * @param refFromEntity  версия, которая ссылается
     * @param oldRefToEntity старая версия, на которую ссылались
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param structureDiff  различие в структурах версий
     */
    private void discoverConflicts(RefBookVersionEntity refFromEntity,
                                   RefBookVersionEntity oldRefToEntity,
                                   RefBookVersionEntity newRefToEntity,
                                   StructureDiff structureDiff) {
        boolean isAltered = isRefBookAltered(structureDiff);

        // NB: CalculateConflictRequest: refFromEntity, oldRefToEntity, newRefToEntity + isAltered
        // NB: CalculateStructureConflictRequest: + refFromReferences, structureDiff && -> isAltered
        if (isAltered) {
            String refCode = oldRefToEntity.getRefBook().getCode();
            List<Structure.Reference> refFromReferences = refFromEntity.getStructure().getRefCodeReferences(refCode);

            createCalculatedDamagedConflicts(refFromEntity, newRefToEntity, refFromReferences, structureDiff);
            createCalculatedAlteredConflicts(refFromEntity, oldRefToEntity, newRefToEntity, refFromReferences);
        }

        createCalculatedDataConflicts(refFromEntity, oldRefToEntity, newRefToEntity, isAltered);
        createRecalculatedConflicts(refFromEntity, oldRefToEntity, newRefToEntity, structureDiff);
    }

    /**
     * Создание конфликтов, связанных с отсутствием кода атрибута в displayExpression.
     *
     * @param refFromEntity     версия, которая ссылается
     * @param newRefToEntity    новая версия, на которую будут ссылаться
     * @param refFromReferences ссылки версии, которая ссылается
     * @param structureDiff     различие в структурах версий
     */
    private void createCalculatedDamagedConflicts(RefBookVersionEntity refFromEntity,
                                                  RefBookVersionEntity newRefToEntity,
                                                  List<Structure.Reference> refFromReferences,
                                                  StructureDiff structureDiff) {
        List<RefBookConflictEntity> entities = calculateDisplayDamagedConflicts(refFromEntity, newRefToEntity, refFromReferences, structureDiff);
        if (!isEmpty(entities)) {
            conflictRepository.saveAll(entities);
        }
    }

    /**
     * Создание конфликтов, связанных с изменением структуры.
     *
     * @param refFromEntity     версия, которая ссылается
     * @param oldRefToEntity    старая версия, на которую ссылаются
     * @param newRefToEntity    новая версия, на которую будут ссылаться
     * @param refFromReferences ссылки версии, которая ссылается
     */
    private void createCalculatedAlteredConflicts(RefBookVersionEntity refFromEntity,
                                                  RefBookVersionEntity oldRefToEntity,
                                                  RefBookVersionEntity newRefToEntity,
                                                  List<Structure.Reference> refFromReferences) {
        SearchDataCriteria searchCriteria = new SearchDataCriteria();
        searchCriteria.setPageSize(REF_BOOK_VERSION_DATA_PAGE_SIZE);

        PageIterator<RefBookRowValue, SearchDataCriteria> pageIterator = new PageIterator<>(
                pageCriteria -> versionService.search(refFromEntity.getId(), searchCriteria),
                searchCriteria, true);
        pageIterator.forEachRemaining(page ->
                refFromReferences.forEach(refFromReference -> {
                    List<RefBookConflictEntity> entities = calculateAlteredConflicts(refFromEntity, oldRefToEntity, newRefToEntity, refFromReference, page.getContent());
                    if (!isEmpty(entities)) {
                        conflictRepository.saveAll(entities);
                    }
                })
        );
    }

    /**
     * Создание конфликтов, связанных с отсутствием кода атрибута в displayExpression.
     *
     * @param refFromEntity     версия, которая ссылается
     * @param newRefToEntity    новая версия, на которую будут ссылаться
     * @param refFromReferences ссылки версии, которая ссылается
     * @param structureDiff     различие в структурах версий
     */
    @SuppressWarnings("WeakerAccess") // NB: public for ConflictServiceTest only.
    public List<RefBookConflictEntity> calculateDisplayDamagedConflicts(RefBookVersionEntity refFromEntity,
                                                                        RefBookVersionEntity newRefToEntity,
                                                                        List<Structure.Reference> refFromReferences,
                                                                        StructureDiff structureDiff) {
        List<String> deletedCodes = getDeletedCodes(structureDiff);
        if (StringUtils.isEmpty(deletedCodes))
            return emptyList();

        return refFromReferences.stream()
                .filter(reference -> containsAnyPlaceholder(reference.getDisplayExpression(), deletedCodes))
                .map(reference ->
                        new RefBookConflictEntity(refFromEntity, newRefToEntity,
                                null, reference.getAttribute(), ConflictType.DISPLAY_DAMAGED))
                .collect(toList());
    }

    /**
     * Создание конфликтов, связанных с изменением структуры.
     *
     * @param refFromEntity    версия, которая ссылается
     * @param oldRefToEntity   старая версия, на которую ссылаются
     * @param newRefToEntity   новая версия, на которую будут ссылаться
     * @param refFromReference ссылка версии, которая ссылается
     * @param refFromRows      строки версии, которая ссылается
     */
    @SuppressWarnings("WeakerAccess") // NB: public for ConflictServiceTest only.
    public List<RefBookConflictEntity> calculateAlteredConflicts(RefBookVersionEntity refFromEntity,
                                                                 RefBookVersionEntity oldRefToEntity,
                                                                 RefBookVersionEntity newRefToEntity,
                                                                 Structure.Reference refFromReference,
                                                                 List<? extends RefBookRowValue> refFromRows) {

        List<Structure.Attribute> oldPrimaries = oldRefToEntity.getStructure().getPrimaries();
        List<Structure.Attribute> newPrimaries = newRefToEntity.getStructure().getPrimaries();
        if (!versionValidation.equalsPrimaries(oldPrimaries, newPrimaries)) {
            return emptyList(); // Для такого случая должен быть отдельный тип конфликта!
        }

        String referenceCode = refFromReference.getAttribute();

        List<RefBookConflictEntity> toAdd = new ArrayList<>(refFromRows.size());

        Collection<RowValue> rowValues = findReferredRowValues(newRefToEntity, newPrimaries, referenceCode, refFromRows);
        if (isEmpty(rowValues))
            return emptyList();

        Map<String, RowValue> referredRowValues = RowUtils.toReferredRowValues(newPrimaries, rowValues);

        for (RowValue refRowValue : refFromRows) {

            Reference fieldReference = RowUtils.getFieldReference(refRowValue, referenceCode);
            if (fieldReference == null || fieldReference.getValue() == null) continue;

            boolean isAltered = isHashChanged(fieldReference, referredRowValues);
            if (isAltered) {
                Long refRecordId = (Long) refRowValue.getSystemId();
                RefBookConflictEntity added = new RefBookConflictEntity(refFromEntity, newRefToEntity,
                        refRecordId, referenceCode, ConflictType.ALTERED);
                toAdd.add(added);
            }
        }

        return toAdd;
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

    private boolean isHashChanged(Reference fieldReference, Map<String, RowValue> referredRowValues) {

        if (StringUtils.isEmpty(fieldReference.getHash()))
            return true;

        RowValue referredRowValue = referredRowValues.get(fieldReference.getValue());
        return referredRowValue != null &&
                !Objects.equals(fieldReference.getHash(), referredRowValue.getHash());
    }

    /**
     * Создание конфликтов, связанных с изменением записей.
     *
     * @param refFromEntity  версия, которая ссылается
     * @param oldRefToEntity старая версия, на которую ссылались
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param isAltered      наличие изменения структуры
     */
    private void createCalculatedDataConflicts(RefBookVersionEntity refFromEntity,
                                               RefBookVersionEntity oldRefToEntity,
                                               RefBookVersionEntity newRefToEntity,
                                               boolean isAltered) {

        CompareDataCriteria compareCriteria = new CompareDataCriteria(oldRefToEntity.getId(), newRefToEntity.getId());
        compareCriteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(
                pageCriteria -> compareService.compareData(pageCriteria).getRows(),
                compareCriteria, true);
        pageIterator.forEachRemaining(page -> {
            List<RefBookConflictEntity> entities = calculateDataDiffConflicts(refFromEntity,
                    oldRefToEntity, newRefToEntity, getDataDiffContent(page, isAltered));
            if (!isEmpty(entities)) {
                conflictRepository.saveAll(entities);
            }
        });
    }

    /**
     * Вычисление конфликтов справочников по diff-записям.
     *
     * @param refFromEntity  версия, которая ссылается
     * @param oldRefToEntity версия, на которую ссылаются
     * @param diffRowValues  diff-записи
     * @return Список конфликтов для версии, которая ссылается
     * @see #checkDataDiffConflicts
     */
    private List<RefBookConflictEntity> calculateDataDiffConflicts(RefBookVersionEntity refFromEntity,
                                                                   RefBookVersionEntity oldRefToEntity,
                                                                   RefBookVersionEntity newRefToEntity,
                                                                   List<DiffRowValue> diffRowValues) {

        List<Structure.Attribute> refToPrimaries = oldRefToEntity.getStructure().getPrimaries();
        List<Structure.Attribute> refFromAttributes = refFromEntity.getStructure().getRefCodeAttributes(oldRefToEntity.getRefBook().getCode());
        List<RefBookRowValue> refFromRowValues = getConflictedRowContent(refFromEntity, diffRowValues, refToPrimaries, refFromAttributes);

        return refFromAttributes.stream().flatMap(refFromAttribute ->
                diffRowValues.stream().flatMap(diffRowValue -> {
                    List<RefBookRowValue> rowValues =
                            findRefBookRowValues(refToPrimaries, refFromAttribute, diffRowValue, refFromRowValues);
                    return rowValues.stream()
                            .map(rowValue ->
                                    new RefBookConflictEntity(refFromEntity, newRefToEntity,
                                            rowValue.getSystemId(), refFromAttribute.getCode(), diffStatusToConflictType(diffRowValue.getStatus())));
                })
        ).collect(toList());
    }

    /**
     * Перевычисление конфликтов.
     *
     * @param refFromEntity  версия, которая ссылается
     * @param oldRefToEntity старая версия, на которую ссылались
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param structureDiff  различие в структурах версий
     */
    private void createRecalculatedConflicts(RefBookVersionEntity refFromEntity,
                                             RefBookVersionEntity oldRefToEntity,
                                             RefBookVersionEntity newRefToEntity,
                                             StructureDiff structureDiff) {
        recalculateStructureConflicts(refFromEntity, oldRefToEntity, newRefToEntity, structureDiff);
        recalculateDataConflicts(refFromEntity, oldRefToEntity, newRefToEntity, structureDiff);
    }

    /**
     * Перевычисление конфликтов структуры.
     *
     * @param refFromEntity  версия, которая ссылается
     * @param oldRefToEntity старая версия, на которую ссылались
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param structureDiff  различие в структурах версий
     */
    private void recalculateStructureConflicts(RefBookVersionEntity refFromEntity,
                                               RefBookVersionEntity oldRefToEntity,
                                               RefBookVersionEntity newRefToEntity,
                                               StructureDiff structureDiff) {

        RefBookConflictCriteria criteria = new RefBookConflictCriteria(refFromEntity.getId(), oldRefToEntity.getId());
        criteria.setConflictTypes(ConflictUtils.getStructureConflictTypes());
        criteria.setOrders(RefBookConflictQueryProvider.getSortRefBookConflicts());
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);

        PageIterator<RefBookConflictEntity, RefBookConflictCriteria> pageIterator = new PageIterator<>(conflictQueryProvider::search, criteria);
        pageIterator.forEachRemaining(page -> {
            List<RefBookConflictEntity> entities = recalculateStructureConflicts(refFromEntity, newRefToEntity, page.getContent(), structureDiff);
            if (!isEmpty(entities)) {
                conflictRepository.saveAll(entities);
            }
        });
    }

    /**
     * Перевычисление существующих конфликтов структуры для справочника.
     *
     * @param refFromEntity  версия справочника, которая ссылается
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param conflicts      список конфликтов
     * @param structureDiff  различие в структурах версий
     * @return Список конфликтов
     */
    private List<RefBookConflictEntity> recalculateStructureConflicts(RefBookVersionEntity refFromEntity,
                                                                      RefBookVersionEntity newRefToEntity,
                                                                      List<? extends RefBookConflictEntity> conflicts,
                                                                      StructureDiff structureDiff) {
        Structure refFromStructure = refFromEntity.getStructure();
        Structure newRefToStructure = newRefToEntity.getStructure();
        boolean isAltered = isRefBookAltered(structureDiff);

        return conflicts.stream()
                .filter(conflict -> ConflictUtils.getStructureConflictTypes().contains(conflict.getConflictType()))
                .filter(conflict -> {
                    Structure.Reference reference = refFromStructure.getReference(conflict.getRefFieldCode());

                    // Если не будет нового конфликта по структуре и
                    // если старый конфликт по структуре не устранён:
                    return !(isAltered && isDisplayDamagedConflict(singletonList(reference), structureDiff))
                            && hasAbsentPlaceholder(reference.getDisplayExpression(), newRefToStructure);
                })
                .map(conflict -> new RefBookConflictEntity(refFromEntity, newRefToEntity,
                        null, conflict.getRefFieldCode(), conflict.getConflictType()))
                .collect(toList());
    }

    /**
     * Перевычисление конфликтов данных.
     *
     * @param refFromEntity  версия, которая ссылается
     * @param oldRefToEntity старая версия, на которую ссылались
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param structureDiff  различие в структурах версий
     */
    private void recalculateDataConflicts(RefBookVersionEntity refFromEntity,
                                          RefBookVersionEntity oldRefToEntity,
                                          RefBookVersionEntity newRefToEntity,
                                          StructureDiff structureDiff) {
        boolean isAltered = isRefBookAltered(structureDiff);

        RefBookConflictCriteria criteria = new RefBookConflictCriteria(refFromEntity.getId(), oldRefToEntity.getId());
        criteria.setConflictTypes(ConflictUtils.getDataConflictTypes());
        criteria.setOrders(RefBookConflictQueryProvider.getSortRefBookConflicts());
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);

        PageIterator<RefBookConflictEntity, RefBookConflictCriteria> pageIterator = new PageIterator<>(conflictQueryProvider::search, criteria);
        pageIterator.forEachRemaining(page -> {
            List<RefBookConflictEntity> entities = recalculateDataConflicts(refFromEntity, oldRefToEntity, newRefToEntity, page.getContent(), isAltered);
            if (!isEmpty(entities)) {
                conflictRepository.saveAll(entities);
            }
        });
    }

    /**
     * Перевычисление существующих конфликтов данных для справочника.
     *
     * @param refFromEntity  версия справочника, которая ссылается
     * @param oldRefToEntity старая версия справочника, на которую ссылались
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param conflicts      страничный список конфликтов
     * @param isAltered      наличие изменения структуры
     * @return Список перевычисленных конфликтов данных для версии, которая ссылается
     */
    // NB: Заменить public на private после исключения вызова из тестов!
    @SuppressWarnings("WeakerAccess")
    public List<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity refFromEntity,
                                                                RefBookVersionEntity oldRefToEntity,
                                                                RefBookVersionEntity newRefToEntity,
                                                                List<? extends RefBookConflictEntity> conflicts,
                                                                boolean isAltered) {
        List<Long> refFromSystemIds = conflicts.stream()
                .map(RefBookConflictEntity::getRefRecordId)
                .filter(Objects::nonNull)
                .collect(toList());
        if (isEmpty(refFromSystemIds))
            return emptyList();

        List<RefBookRowValue> refFromRowValues = getSystemRowValues(refFromEntity.getId(), refFromSystemIds);
        List<ReferenceFilterValue> filterValues = toFilterValues(refFromEntity, oldRefToEntity, conflicts, refFromRowValues);
        List<DiffRowValue> diffRowValues = toDiffRowValues(oldRefToEntity.getId(), newRefToEntity.getId(), filterValues);

        List<RefBookConflictEntity> filteredConflicts = conflicts.stream()
                // Если структура не изменена, то для перевычисления нужны все конфликты.
                // Если же структура изменена, то все строки с заполненными ссылками помечаются как ALTERED-конфликтные,
                // поэтому для перевычисления достаточно отработать только удалённые конфликты
                // (see details in javadoc of ConflictServiceTest#testRecalculateConflicts).
                .filter(conflict -> !isAltered || ALTERED_RECALCULATING_CONFLICT_TYPES.contains(conflict.getConflictType()))
                .collect(toList());

        return recalculateDataConflicts(refFromEntity, oldRefToEntity, newRefToEntity, filteredConflicts, refFromRowValues, diffRowValues);
    }

    /**
     * Перевычисление существующих конфликтов данных для справочника.
     *
     * @param refFromEntity    версия справочника, которая ссылается
     * @param oldRefToEntity   старая версия справочника, на которую ссылались
     * @param newRefToEntity   новая версия, на которую будут ссылаться
     * @param conflicts        список конфликтов
     * @param refFromRowValues список записей версии справочника, которая ссылается
     * @param diffRowValues    список различий версий справочника, на которую ссылаются
     * @return Список конфликтов
     */
    private List<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity refFromEntity,
                                                                 RefBookVersionEntity oldRefToEntity,
                                                                 RefBookVersionEntity newRefToEntity,
                                                                 List<RefBookConflictEntity> conflicts,
                                                                 List<RefBookRowValue> refFromRowValues,
                                                                 List<DiffRowValue> diffRowValues) {
        return conflicts.stream()
                .filter(conflict -> ConflictUtils.getDataConflictTypes().contains(conflict.getConflictType()))
                .map(conflict -> {
                    RefBookRowValue refFromRowValue = findConflictedValue(refFromRowValues, conflict);
                    if (refFromRowValue == null)
                        return null;

                    return recalculateDataConflict(refFromEntity, oldRefToEntity, newRefToEntity, conflict, refFromRowValue, diffRowValues);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Перевычисление существующих конфликтов справочников.
     *
     * @param refFromEntity   версия справочника, которая ссылается
     * @param oldRefToEntity  версия справочника, на которую ссылались
     * @param conflict        конфликт
     * @param refFromRowValue запись версии справочника, которая ссылается
     * @param diffRowValues   список различий версий справочника, на которую ссылаются
     * @return Список конфликтов
     */
    private RefBookConflictEntity recalculateDataConflict(RefBookVersionEntity refFromEntity,
                                                          RefBookVersionEntity oldRefToEntity,
                                                          RefBookVersionEntity newRefToEntity,
                                                          RefBookConflictEntity conflict,
                                                          RefBookRowValue refFromRowValue,
                                                          List<DiffRowValue> diffRowValues) {

        ReferenceFieldValue fieldValue = (ReferenceFieldValue) (refFromRowValue.getFieldValue(conflict.getRefFieldCode()));
        Structure.Reference refFromReference = refFromEntity.getStructure().getReference(conflict.getRefFieldCode());
        Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(oldRefToEntity.getStructure());
        ReferenceFilterValue filterValue = new ReferenceFilterValue(refToAttribute, fieldValue);

        // NB: Extract to separated method `recalculateDataConflict`.
        // Проверка существующего конфликта с текущей diff-записью по правилам пересчёта.
        DiffRowValue diffRowValue;
        switch (conflict.getConflictType()) {
            case DELETED:
                diffRowValue = findDiffRowValue(filterValue, diffRowValues);
                if (Objects.nonNull(diffRowValue)) {
                    if (DiffStatusEnum.INSERTED.equals(diffRowValue.getStatus())) {
                        String displayValue = diffValuesToDisplayValue(refFromReference.getDisplayExpression(),
                                diffRowValue.getValues(), DiffStatusEnum.INSERTED);

                        if (Objects.equals(displayValue, fieldValue.getValue().getDisplayValue()))
                            return null; // Восстановление удалённой строки

                        conflict.setConflictType(ConflictType.UPDATED); // Вставка удалённой строки с изменениями

                    } else
                        return null; // Для удалённых записей не может быть удалений и обновлений
                }

                break; // Есть только старое удаление

            case UPDATED:
            case ALTERED:
                diffRowValue = findDiffRowValue(filterValue, diffRowValues);
                if (Objects.nonNull(diffRowValue))
                    return null; // Есть новые изменения

                break; // Есть только старое обновление

            default:
                return null; // Нет старых конфликтов, только новые
        }

        return new RefBookConflictEntity(refFromEntity, newRefToEntity,
                refFromRowValue.getSystemId(), conflict.getRefFieldCode(), conflict.getConflictType());
    }

    /**
     * Проверка на наличие конфликта DISPLAY_DAMAGED.
     *
     * @param references    список ссылок версии, которая ссылается
     * @param structureDiff различие в структурах версий
     * @return Наличие конфликта
     */
    private boolean isDisplayDamagedConflict(List<Structure.Reference> references, StructureDiff structureDiff) {

        List<String> deletedCodes = getDeletedCodes(structureDiff);
        return !StringUtils.isEmpty(deletedCodes)
                && references.stream().anyMatch(reference -> containsAnyPlaceholder(reference.getDisplayExpression(), deletedCodes));
    }

    /**
     * Получение кодов удалённых атрибутов.
     *
     * @param structureDiff различие в структурах версий
     * @return Список кодов
     */
    private List<String> getDeletedCodes(StructureDiff structureDiff) {
        return structureDiff.getDeleted().stream()
                .map(deleted -> deleted.getOldAttribute().getCode())
                .collect(toList());
    }

    private RefBookVersionEntity findVersionOrThrow(Integer id) {

        RefBookVersionEntity entity = (id != null) ? versionRepository.findById(id).orElse(null) : null;
        if (entity == null)
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, id));

        return entity;
    }
}
