package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.model.conflict.*;
import ru.inovus.ms.rdm.model.diff.StructureDiff;
import ru.inovus.ms.rdm.model.field.ReferenceFilterValue;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.queryprovider.RefBookConflictQueryProvider;
import ru.inovus.ms.rdm.repository.RefBookConflictRepository;
import ru.inovus.ms.rdm.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.ModelGenerator;
import ru.inovus.ms.rdm.util.PageIterator;
import ru.inovus.ms.rdm.util.ReferrerEntityIteratorProvider;
import ru.inovus.ms.rdm.util.StructureUtils;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.ComparableUtils.*;
import static ru.inovus.ms.rdm.util.ConflictUtils.conflictTypeToDiffStatus;
import static ru.inovus.ms.rdm.util.ConflictUtils.diffStatusToConflictType;
import static ru.inovus.ms.rdm.util.ConverterUtil.field;
import static ru.inovus.ms.rdm.util.ConverterUtil.fields;
import static ru.inovus.ms.rdm.util.FieldValueUtils.*;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private static final int REF_BOOK_VERSION_PAGE_SIZE = 100;
    static final int REF_BOOK_VERSION_DATA_PAGE_SIZE = 100;

    private static final List<DiffStatusEnum> CALCULATING_DIFF_STATUSES = asList(DiffStatusEnum.DELETED, DiffStatusEnum.UPDATED);
    private static final List<ConflictType> RECALCULATING_CONFLICT_TYPES = asList(ConflictType.UPDATED, ConflictType.ALTERED);

    static final List<Sort.Order> SORT_VERSION_DATA = singletonList(
            new Sort.Order(Sort.Direction.ASC, DataConstants.SYS_PRIMARY_COLUMN)
    );

    private RefBookVersionRepository versionRepository;
    private RefBookConflictRepository conflictRepository;
    private RefBookConflictQueryProvider conflictQueryProvider;

    private CompareService compareService;
    private SearchDataService searchDataService;

    private VersionService versionService;
    private VersionValidation versionValidation;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public ConflictServiceImpl(RefBookVersionRepository versionRepository,
                               RefBookConflictRepository conflictRepository, RefBookConflictQueryProvider conflictQueryProvider,
                               CompareService compareService, SearchDataService searchDataService,
                               VersionService versionService, VersionValidation versionValidation) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;
        this.conflictQueryProvider = conflictQueryProvider;

        this.compareService = compareService;
        this.searchDataService = searchDataService;

        this.versionService = versionService;
        this.versionValidation = versionValidation;
    }

    /**
     * Вычисление конфликтов справочников по критерию.
     *
     * @param criteria критерий вычисления
     * @return Список конфликтов
     */
    @Override
    @Transactional(readOnly = true)
    // NB: for ApplicationTest only.
    public List<RefBookConflict> calculateDataConflicts(CalculateConflictCriteria criteria) {

        List<RefBookConflictEntity> list = new ArrayList<>();

        RefBookVersionEntity refFromEntity = versionRepository.getOne(criteria.getReferrerVersionId());
        RefBookVersionEntity oldRefToEntity = versionRepository.getOne(criteria.getOldVersionId());
        RefBookVersionEntity newRefToEntity = versionRepository.getOne(criteria.getNewVersionId());

        CompareDataCriteria dataCriteria = new CompareDataCriteria(criteria.getOldVersionId(), criteria.getNewVersionId());
        dataCriteria.setOrders(SORT_VERSION_DATA);
        dataCriteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

        Function<CompareDataCriteria, Page<DiffRowValue>> pageSource = pageCriteria -> compareService.compareData(pageCriteria).getRows();
        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(pageSource, dataCriteria);
        pageIterator.forEachRemaining(page -> {
            List<RefBookConflictEntity> entities = calculateDataDiffConflicts(refFromEntity,
                    oldRefToEntity, newRefToEntity, getDataDiffContent(page, criteria.getStructureAltered()));
            list.addAll(entities);
        });

        return list.stream().map(this::refBookConflictModel).collect(toList());
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

        List<Structure.Attribute> refToPrimaries = oldRefToEntity.getStructure().getPrimary();
        List<Structure.Attribute> refFromAttributes = refFromEntity.getStructure().getRefCodeAttributes(oldRefToEntity.getRefBook().getCode());
        List<RefBookRowValue> refFromRowValues = getConflictedRowContent(refFromEntity, diffRowValues, refToPrimaries, refFromAttributes);

        return refFromAttributes.stream()
                .flatMap(refFromAttribute ->
                        diffRowValues.stream()
                                .flatMap(diffRowValue -> {
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

        versionValidation.validateVersionExists(refFromId);
        versionValidation.validateVersionPairExists(oldRefToId, newRefToId);

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity oldRefToEntity = versionRepository.getOne(oldRefToId);

        if (ConflictType.ALTERED.equals(conflictType) || ConflictType.DISPLAY_DAMAGED.equals(conflictType)) {
            StructureDiff structureDiff = compareService.compareStructures(oldRefToId, newRefToId);

            // NB: to-do: Проверить сначала, есть ли реальные ссылки из refFromId ?!
            if (ConflictType.ALTERED.equals(conflictType))
                return isRefBookAltered(structureDiff);

            List<Structure.Reference> refFromReferences = refFromEntity.getStructure().getRefCodeReferences(oldRefToEntity.getRefBook().getCode());
            return isDisplayDamagedConflict(refFromReferences, structureDiff);
        }

        DiffStatusEnum diffStatus = conflictTypeToDiffStatus(conflictType);

        CompareDataCriteria criteria = new CompareDataCriteria(oldRefToId, newRefToId);
        criteria.setOrders(SORT_VERSION_DATA);
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

        Function<CompareDataCriteria, Page<DiffRowValue>> pageSource = pageCriteria -> compareService.compareData(pageCriteria).getRows();
        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        while (pageIterator.hasNext()) {
            Page<DiffRowValue> page = pageIterator.next();
            if (checkDataDiffConflicts(refFromEntity, oldRefToEntity, getDataDiffContent(page, false), diffStatus))
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
    private Boolean checkDataDiffConflicts(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                           List<DiffRowValue> diffRowValues, DiffStatusEnum diffStatus) {

        List<Structure.Attribute> refToPrimaries = refToEntity.getStructure().getPrimary();
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
     * Поиск конфликтов по критерию поиска.
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

    @Override
    public RefBookConflict findDataConflict(Integer refFromId, String refFieldCode, Long rowSystemId) {

        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(refFromId);
        criteria.setIsLastPublishedVersion(true);
        criteria.setRefFieldCode(refFieldCode);
        criteria.setRefRecordId(rowSystemId);
        criteria.setPageSize(1);

        Page<RefBookConflict> conflicts = search(criteria);
        return (conflicts != null && !isEmpty(conflicts.getContent())) ? conflicts.getContent().get(0) : null;
    }

    /**
     * Получение конфликтных идентификаторов для версии, которая ссылается,
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
     * Перевычисление существующих конфликтов данных для справочника.
     *
     * @param refFromEntity  версия справочника, которая ссылается
     * @param oldRefToEntity старая версия справочника, на которую ссылались
     * @param newRefToEntity новая версия, на которую будут ссылаться
     * @param conflicts      страничный список конфликтов
     * @param isAltered      наличие изменения структуры
     * @return Список перевычисленных конфликтов данных для версии, которая ссылается
     */
    @SuppressWarnings("WeakerAccess")
    public List<RefBookConflictEntity> recalculateDataConflicts(RefBookVersionEntity refFromEntity,
                                                                RefBookVersionEntity oldRefToEntity,
                                                                RefBookVersionEntity newRefToEntity,
                                                                List<RefBookConflictEntity> conflicts,
                                                                boolean isAltered) {
        List<Long> refFromSystemIds = conflicts.stream()
                .map(RefBookConflictEntity::getRefRecordId)
                .filter(Objects::nonNull)
                .collect(toList());
        if (isEmpty(refFromSystemIds))
            return emptyList();

        List<RefBookRowValue> refFromRowValues = getSystemRowValues(refFromEntity.getId(), refFromSystemIds);
        List<ReferenceFilterValue> filterValues = toFilterValues(refFromEntity, oldRefToEntity, conflicts, refFromRowValues);
        List<DiffRowValue> diffRowValues = getRefToDiffRowValues(oldRefToEntity.getId(), newRefToEntity.getId(), filterValues);

        List<RefBookConflictEntity> filteredConflicts = conflicts.stream()
                // NB: Если структура изменена, то все строки помечаются как ALTERED-конфликтные,
                // поэтому для перевычисления достаточно отработать только удалённые конфликты
                // (see details in javadoc of ConflictServiceTest#testRecalculateConflicts).
                .filter(conflict -> !(isAltered && RECALCULATING_CONFLICT_TYPES.contains(conflict.getConflictType())))
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
                .map(conflict -> {
                    if (conflict.isDisplayDamaged()) {
                        // NB: Analyze dipslayExpression ?!
                        return new RefBookConflictEntity(refFromEntity, newRefToEntity,
                                null, conflict.getRefFieldCode(), conflict.getConflictType());
                    }

                    RefBookRowValue refFromRowValue = refFromRowValues.stream()
                            .filter(rowValue -> rowValue.getSystemId().equals(conflict.getRefRecordId()))
                            .findFirst().orElse(null);
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
                break; // Нет старых конфликтов, только новые.
        }

        return new RefBookConflictEntity(refFromEntity, newRefToEntity,
                refFromRowValue.getSystemId(), conflict.getRefFieldCode(), conflict.getConflictType());
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

        versionValidation.validateVersionPairExists(oldVersionId, newVersionId);

        RefBookVersionEntity oldRefToEntity = versionRepository.getOne(oldVersionId);
        RefBookVersionEntity newRefToEntity = versionRepository.getOne(newVersionId);
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

        versionValidation.validateVersionPairExists(oldVersionId, newVersionId);

        if (!newVersionId.equals(oldVersionId))
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
        Set<List<FieldSearchCriteria>> filters = createFiltersForDiffRowValues(diffRowValues, refToPrimaries, refFromAttributes);
        DataCriteria criteria = new DataCriteria(refFromEntity.getStorageCode(),
                refFromEntity.getFromDate(), refFromEntity.getToDate(),
                fields(refFromEntity.getStructure()), filters, null);
        // NB: Get all required rows because filters.size() <= REF_BOOK_DIFF_CONFLICT_PAGE_SIZE.
        criteria.setPage(0);
        criteria.setSize(0);

        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(criteria);
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

        SearchDataCriteria criteria = new SearchDataCriteria(toSystemIdFilters(systemIds), null);
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);

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
    private List<DiffRowValue> getRefToDiffRowValues(Integer oldVersionId, Integer newVersionId, List<ReferenceFilterValue> filterValues) {

        CompareDataCriteria criteria = new CompareDataCriteria(oldVersionId, newVersionId);
        criteria.setPrimaryAttributesFilters(toAttributeFilters(filterValues));
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);

        return compareService.compareData(criteria).getRows().getContent();
    }

    /**
     * Получение diff-записей данных версий справочников для конфликтов.
     *
     * @param diffRowValues список всех различий
     * @param isAltered     наличие изменения структуры
     * @return Список различий
     */
    private List<DiffRowValue> getDataDiffContent(Page<DiffRowValue> diffRowValues, boolean isAltered) {
        return diffRowValues.getContent().stream()
                .filter(diffRowValue -> isAltered
                        ? DiffStatusEnum.DELETED.equals(diffRowValue.getStatus())
                        : CALCULATING_DIFF_STATUSES.contains(diffRowValue.getStatus()))
                .collect(toList());
    }

    /**
     * Получение ссылочных значений для фильтрации
     *
     * @param refFromEntity    версия справочника, которая ссылается
     * @param refToEntity      версия справочника, на которую ссылались
     * @param conflicts        список конфликтов
     * @param refFromRowValues список записей версии справочника, которая ссылается
     * @return Список ссылочных значений
     */
    private List<ReferenceFilterValue> toFilterValues(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                                      List<RefBookConflictEntity> conflicts, List<RefBookRowValue> refFromRowValues) {
        return conflicts.stream()
                .filter(conflict -> Objects.nonNull(conflict.getRefRecordId()))
                .map(conflict -> {
                    RefBookRowValue refBookRowValue = refFromRowValues.stream()
                            .filter(rowValue -> rowValue.getSystemId().equals(conflict.getRefRecordId()))
                            .findFirst().orElse(null);
                    if (refBookRowValue == null)
                        return null;

                    Structure.Reference refFromReference = refFromEntity.getStructure().getReference(conflict.getRefFieldCode());
                    Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(refToEntity.getStructure());
                    ReferenceFieldValue fieldValue = (ReferenceFieldValue) (refBookRowValue.getFieldValue(conflict.getRefFieldCode()));
                    return new ReferenceFilterValue(refToAttribute, fieldValue);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Создание фильтров для получения записей данных версии справочника по первичным ключам.
     *
     * @param diffRowValues     diff-записи
     * @param refToPrimaries    первичные ключи справочника, на который ссылаются
     * @param refFromAttributes ссылочные атрибуты версии, которая ссылается
     * @return Множество списков фильтров
     */
    private Set<List<FieldSearchCriteria>> createFiltersForDiffRowValues(List<DiffRowValue> diffRowValues,
                                                                         List<Structure.Attribute> refToPrimaries,
                                                                         List<Structure.Attribute> refFromAttributes) {
        return diffRowValues.stream()
                .flatMap(diff -> {
                    DiffFieldValue diffFieldValue = diff.getDiffFieldValue(refToPrimaries.get(0).getCode());
                    Object value = getDiffFieldValue(diffFieldValue, diff.getStatus());

                    return refFromAttributes.stream()
                            .map(attribute ->
                                    singletonList(new FieldSearchCriteria(field(attribute), SearchTypeEnum.EXACT, singletonList(value)))
                            );
                }).collect(toSet());
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
            List<Structure.Reference> refFromReferences = refFromEntity.getStructure().getRefCodeReferences(oldRefToEntity.getRefBook().getCode());

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
        if (!isEmpty(entities))
            conflictRepository.saveAll(entities);
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
        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setOrders(SORT_VERSION_DATA);
        criteria.setPageSize(REF_BOOK_VERSION_DATA_PAGE_SIZE);

        Function<SearchDataCriteria, Page<RefBookRowValue>> pageSource = pageCriteria -> versionService.search(refFromEntity.getId(), criteria);
        PageIterator<RefBookRowValue, SearchDataCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page ->
            refFromReferences.forEach(refFromReference -> {
                List<RefBookConflictEntity> entities = calculateAlteredConflicts(refFromEntity, oldRefToEntity, newRefToEntity, refFromReference, page.getContent());
                if (!isEmpty(entities))
                    conflictRepository.saveAll(entities);
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
        List<String> deletedCodes = structureDiff.getDeleted().stream()
                .map(attributeDiff -> attributeDiff.getOldAttribute().getCode())
                .collect(toList());
        if (StringUtils.isEmpty(deletedCodes))
            return emptyList();

        return refFromReferences.stream()
                .filter(reference -> StructureUtils.containsAnyPlaceholder(reference.getDisplayExpression(), deletedCodes))
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
                                                                 List<RefBookRowValue> refFromRows) {
        Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(oldRefToEntity.getStructure());

        List<AbstractMap.SimpleEntry<Long, ReferenceFieldValue>> fieldEntries = refFromRows.stream()
                .map(refFromRow -> {
                    ReferenceFieldValue referenceFieldValue = (ReferenceFieldValue) (refFromRow.getFieldValue(refFromReference.getAttribute()));
                    if (Objects.isNull(referenceFieldValue)
                            || StringUtils.isEmpty(referenceFieldValue.getValue()))
                        return null;

                    return new AbstractMap.SimpleEntry<>(refFromRow.getSystemId(), referenceFieldValue);
                })
                .filter(Objects::nonNull)
                .collect(toList());

        List<ReferenceFilterValue> filterValues = fieldEntries.stream()
                .map(fieldEntry -> new ReferenceFilterValue(refToAttribute, fieldEntry.getValue()))
                .collect(toList());
        List<RefBookRowValue> refToRowValues = getRefToRowValues(newRefToEntity.getId(), filterValues);

        return fieldEntries.stream()
                .map(fieldEntry -> {
                    Object castedFieldValue = castFieldValue(fieldEntry.getValue(), refToAttribute.getType());
                    if (isFieldValueRow(refToAttribute.getCode(), castedFieldValue, refToRowValues)) {
                        return new RefBookConflictEntity(refFromEntity, newRefToEntity,
                                fieldEntry.getKey(), fieldEntry.getValue().getField(), ConflictType.ALTERED);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Получение записей по значениям ссылки.
     *
     * @param versionId    идентификатор версии справочника
     * @param filterValues список ссылочных значений, по которым выполняется поиск
     * @return Список записей
     */
    private List<RefBookRowValue> getRefToRowValues(Integer versionId, List<ReferenceFilterValue> filterValues) {
        if (versionId == null || isEmpty(filterValues))
            return emptyList();

        SearchDataCriteria criteria = new SearchDataCriteria(toAttributeFilters(filterValues), null);
        criteria.setPageSize(REF_BOOK_VERSION_DATA_PAGE_SIZE);

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return (rowValues != null && !isEmpty(rowValues.getContent())) ? rowValues.getContent() : emptyList();
    }

    /**
     * Сохранение информации о вычисленных конфликтах версии.
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

        CompareDataCriteria criteria = new CompareDataCriteria(oldRefToEntity.getId(), newRefToEntity.getId());
        criteria.setOrders(SORT_VERSION_DATA);
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

        Function<CompareDataCriteria, Page<DiffRowValue>> pageSource = pageCriteria -> compareService.compareData(pageCriteria).getRows();
        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<RefBookConflictEntity> entities = calculateDataDiffConflicts(refFromEntity,
                    oldRefToEntity, newRefToEntity, getDataDiffContent(page, isAltered));
            if (!isEmpty(entities))
                conflictRepository.saveAll(entities);
        });
    }

    /**
     * Сохранение информации о перевычисленных конфликтах.
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
     * Сохранение информации о конфликтах структуры.
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
        criteria.setConflictType(ConflictType.DISPLAY_DAMAGED);
        criteria.setOrders(RefBookConflictQueryProvider.getSortRefBookConflicts());
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);

        Function<RefBookConflictCriteria, Page<RefBookConflictEntity>> pageSource = conflictQueryProvider::search;
        PageIterator<RefBookConflictEntity, RefBookConflictCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<RefBookConflictEntity> entities = recalculateStructureConflicts(refFromEntity, newRefToEntity, page.getContent(), structureDiff);
            if (!isEmpty(entities))
                conflictRepository.saveAll(entities);
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
                                                                      List<RefBookConflictEntity> conflicts,
                                                                      StructureDiff structureDiff) {
        Structure refFromStructure = refFromEntity.getStructure();
        boolean isAltered = isRefBookAltered(structureDiff);

        return conflicts.stream()
                .filter(conflict -> {
                    if (!isAltered)
                        return true;

                    Structure.Reference reference = refFromStructure.getReference(conflict.getRefFieldCode());
                    return !isDisplayDamagedConflict(singletonList(reference), structureDiff);
                })
                .map(conflict -> new RefBookConflictEntity(refFromEntity, newRefToEntity,
                        null, conflict.getRefFieldCode(), conflict.getConflictType()))
                .collect(toList());
    }

    /**
     * Сохранение информации о перевычисленных конфликтах данных.
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
        criteria.setConflictTypes(List.of(ConflictType.UPDATED, ConflictType.DELETED, ConflictType.ALTERED));
        criteria.setOrders(RefBookConflictQueryProvider.getSortRefBookConflicts());
        criteria.setPageSize(RefBookConflictQueryProvider.REF_BOOK_CONFLICT_PAGE_SIZE);

        Function<RefBookConflictCriteria, Page<RefBookConflictEntity>> pageSource = conflictQueryProvider::search;
        PageIterator<RefBookConflictEntity, RefBookConflictCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<RefBookConflictEntity> entities = recalculateDataConflicts(refFromEntity, oldRefToEntity, newRefToEntity, page.getContent(), isAltered);
            if (!isEmpty(entities))
                conflictRepository.saveAll(entities);
        });
    }

    /**
     * Проверка на наличие конфликта DISPLAY_DAMAGED.
     *
     * @param references    список ссылок версии, которая ссылается
     * @param structureDiff различие в структурах версий
     * @return Наличие конфликта
     */
    private static boolean isDisplayDamagedConflict(List<Structure.Reference> references, StructureDiff structureDiff) {
        List<String> deletedAttributeCodes = structureDiff.getDeleted().stream()
                .map(deleted -> deleted.getOldAttribute().getCode())
                .collect(toList());
        return isDisplayDamagedConflict(references, deletedAttributeCodes);
    }

    /**
     * Проверка на наличие конфликта DISPLAY_DAMAGED.
     *
     * @param references     список ссылок версии, которая ссылается
     * @param attributeCodes список кодов атрибутов, влияющих на ссылки
     * @return Наличие конфликта
     */
    private static boolean isDisplayDamagedConflict(List<Structure.Reference> references, List<String> attributeCodes) {
        return references.stream()
                .anyMatch(reference -> {
                    DisplayExpression expression = new DisplayExpression(reference.getDisplayExpression());
                    return CollectionUtils.containsAny(attributeCodes, expression.getPlaceholders());
                });
    }
}
