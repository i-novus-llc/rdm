package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.repositiory.RefBookConflictRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.ConflictUtils;
import ru.inovus.ms.rdm.util.RowUtils;
import ru.inovus.ms.rdm.validation.VersionValidation;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.util.ComparableUtils.castRefValue;
import static ru.inovus.ms.rdm.util.ComparableUtils.findRefBookRowValue;
import static ru.inovus.ms.rdm.util.ComparableUtils.findRefBookRowValues;
import static ru.inovus.ms.rdm.util.ConflictUtils.conflictTypeToDiffStatus;
import static ru.inovus.ms.rdm.util.ConflictUtils.diffStatusToConflictType;
import static ru.inovus.ms.rdm.util.ConverterUtil.fields;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private static final String REFBOOK_DRAFT_NOT_FOUND = "refbook.draft.not.found";
    private static final String VERSION_IS_NOT_DRAFT = "version.is.not.draft";
    private static final String VERSION_IS_NOT_LAST_PUBLISHED = "version.is.not.last.published";
    private static final String CONFLICT_NOT_FOUND = "conflict.not.found";
    private static final String CONFLICTED_FROM_ROW_NOT_FOUND = "conflicted.from.row.not.found";
    private static final String CONFLICTED_TO_ROW_NOT_FOUND = "conflicted.to.row.not.found";
    private static final String CONFLICTED_REFERENCE_NOT_FOUND = "conflicted.reference.row.not.found";

    private RefBookConflictRepository conflictRepository;

    private CompareService compareService;
    private DraftDataService draftDataService;
    private SearchDataService searchDataService;

    private RefBookService refBookService;
    private VersionService versionService;
    private DraftService draftService;

    private VersionValidation versionValidation;
    private RefBookVersionRepository versionRepository;

    @Autowired
    @SuppressWarnings("all")
    public ConflictServiceImpl(RefBookConflictRepository conflictRepository,
                               CompareService compareService,
                               DraftDataService draftDataService,
                               SearchDataService searchDataService,
                               RefBookService refBookService,
                               VersionService versionService,
                               DraftService draftService,
                               VersionValidation versionValidation,
                               RefBookVersionRepository versionRepository) {
        this.conflictRepository = conflictRepository;

        this.compareService = compareService;
        this.draftDataService = draftDataService;
        this.searchDataService = searchDataService;

        this.refBookService = refBookService;
        this.versionService = versionService;
        this.draftService = draftService;

        this.versionValidation = versionValidation;
        this.versionRepository = versionRepository;
    }

    /**
     * Вычисление конфликтов справочников при наличии ссылочных атрибутов.
     *
     * @see #checkDiffConflicts
     */
    @Override
    @Transactional(readOnly = true)
    public List<Conflict> calculateConflicts(Integer refFromId, Integer refToId) {

        versionValidation.validateVersionExists(refFromId);
        versionValidation.validateVersionExists(refToId);

        RefBookVersionEntity refToEntity = versionRepository.getOne(refToId);
        Integer refToDraftId = getRefBookDraftVersion(refToEntity.getRefBook().getId()).getId();

        return calculateConflicts(refFromId, refToId, refToDraftId);
    }

    /**
     * Вычисление конфликтов справочников при наличии ссылочных атрибутов.
     *
     * @param refFromId  идентификатор версии, которая ссылается
     * @param oldRefToId идентификатор старой версии, на которую ссылались
     * @param newRefToId идентификатор новой версии, на которую будут ссылаться
     * @return Список конфликтов для версии, которая ссылается
     */
    private List<Conflict> calculateConflicts(Integer refFromId, Integer oldRefToId, Integer newRefToId) {

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity refToEntity = versionRepository.getOne(oldRefToId);

        return createDiffConflicts(getDataDiffContent(oldRefToId, newRefToId),
                getDataAllRowContent(refFromEntity),
                refToEntity.getStructure(),
                refFromEntity.getStructure(),
                getRefAttributes(refFromEntity.getStructure(), refToEntity.getRefBook().getCode())
        );
    }

    private List<Conflict> createDiffConflicts(List<DiffRowValue> diffRowValues, List<RefBookRowValue> refFromRowValues,
                                               Structure refToStructure, Structure refFromStructure,
                                               List<Structure.Attribute> refFromAttributes) {
        return refFromAttributes
                .stream()
                .flatMap(refFromAttribute ->
                        diffRowValues
                                .stream()
                                .filter(diffRowValue ->
                                        asList(DiffStatusEnum.DELETED, DiffStatusEnum.UPDATED)
                                                .contains(diffRowValue.getStatus()))
                                .flatMap(diffRowValue -> {
                                    List<RefBookRowValue> rowValues =
                                            findRefBookRowValues(refToStructure.getPrimary(), refFromAttribute,
                                                    diffRowValue, refFromRowValues);
                                    return rowValues.stream()
                                            .map(rowValue ->
                                                    createDiffConflict(diffRowValue, rowValue, refFromAttribute, refFromStructure));
                                })
                ).collect(toList());
    }

    private Conflict createDiffConflict(DiffRowValue diffRowValue, RefBookRowValue refFromRowValue,
                                        Structure.Attribute refFromAttribute, Structure refFromStructure) {
        Conflict conflict = new Conflict();
        conflict.setRefAttributeCode(refFromAttribute.getCode());
        conflict.setConflictType(diffStatusToConflictType(diffRowValue.getStatus()));
        conflict.setPrimaryValues(getRowPrimaryValues(refFromRowValue, refFromStructure));

        return conflict;
    }

    /**
     * Проверка на наличие конфликта справочников при наличии ссылочных атрибутов.
     *
     * @see #calculateConflicts
     */
    @Override
    @Transactional(readOnly = true)
    public Boolean checkConflicts(Integer refFromId, Integer refToId, ConflictType conflictType) {

        versionValidation.validateVersionExists(refFromId);
        versionValidation.validateVersionExists(refToId);

        RefBookVersionEntity refToEntity = versionRepository.getOne(refToId);
        Integer refToLastPublishedId = versionService.getLastPublishedVersion(refToEntity.getRefBook().getCode()).getId();

        return checkConflicts(refFromId, refToLastPublishedId, refToId, conflictType);
    }

    /**
     * Проверка на наличие конфликта справочников при наличии ссылочных атрибутов.
     *
     * @param refFromId  идентификатор версии, которая ссылается
     * @param oldRefToId идентификатор старой версии, на которую ссылаются
     * @param newRefToId идентификатор новой версии, на которую будут ссылаться
     * @return Наличие конфликтов
     */
    private Boolean checkConflicts(Integer refFromId, Integer oldRefToId, Integer newRefToId, ConflictType conflictType) {

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity refToEntity = versionRepository.getOne(oldRefToId);

        return checkDiffConflicts(getDataDiffContent(oldRefToId, newRefToId),
                getDataAllRowContent(refFromEntity),
                refToEntity.getStructure(),
                getRefAttributes(refFromEntity.getStructure(), refToEntity.getRefBook().getCode()),
                conflictTypeToDiffStatus(conflictType)
        );
    }

    private Boolean checkDiffConflicts(List<DiffRowValue> diffRowValues, List<RefBookRowValue> refFromRowValues,
                                       Structure refToStructure, List<Structure.Attribute> refFromAttributes,
                                       DiffStatusEnum diffStatus) {
        return refFromAttributes
                .stream()
                .anyMatch(refFromAttribute ->
                        diffRowValues
                                .stream()
                                .filter(diffRowValue ->
                                        diffStatus.equals(diffRowValue.getStatus()))
                                .anyMatch(diffRowValue -> {
                                    RefBookRowValue rowValue =
                                            findRefBookRowValue(refToStructure.getPrimary(), refFromAttribute,
                                                    diffRowValue, refFromRowValues);
                                    return rowValue != null;
                                })
                );
    }

    /**
     * Получение справочников, имеющих конфликты с проверяемым справочником.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RefBookVersion> getConflictReferrers(Integer versionId, ConflictType conflictType) {

        versionValidation.validateVersionExists(versionId);

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        List<RefBookVersion> referrers = refBookService.getReferrerVersions(versionEntity.getRefBook().getCode(), RefBookSourceType.LAST_VERSION, null);
        return referrers.stream()
                .filter(referrer -> checkConflicts(referrer.getId(), versionId, conflictType))
                .collect(Collectors.toList());
    }

    /**
     * Сохранение информации о конфликтах.
     *
     * @param refFromId идентификатор черновика справочника со ссылками
     * @param refToId   идентификатор версии изменённого справочника
     * @param conflicts список конфликтов
     */
    @Override
    @Transactional
    public void create(Integer refFromId, Integer refToId, List<Conflict> conflicts) {
        if (CollectionUtils.isEmpty(conflicts))
            throw new NotFoundException(CONFLICT_NOT_FOUND);

        conflicts.forEach(conflict -> create(refFromId, refToId, conflict));
    }

    private Conflict create(Integer refFromId, Integer refToId, Conflict conflict) {
        if (conflict == null || conflict.isEmpty())
            return null;

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity refToEntity = versionRepository.getOne(refToId);

        RefBookRowValue refFromRowValue = getRefFromRowValue(refFromEntity, conflict.getPrimaryValues());
        if (refFromRowValue == null)
            throw new NotFoundException(CONFLICTED_FROM_ROW_NOT_FOUND);

        return create(refFromEntity, refToEntity, refFromRowValue, conflict);
    }

    private Conflict create(RefBookVersionEntity referrerEntity, RefBookVersionEntity publishedEntity,
                            RefBookRowValue referrerRowValue, Conflict conflict) {
        RefBookConflictEntity entity = new RefBookConflictEntity();
        entity.setReferrerVersion(referrerEntity);
        entity.setPublishedVersion(publishedEntity);

        entity.setRefRecordId(referrerRowValue.getSystemId());
        entity.setRefFieldCode(conflict.getRefAttributeCode());
        entity.setConflictType(conflict.getConflictType());

        RefBookConflictEntity savedEntity = conflictRepository.save(entity);

        return conflictModel(savedEntity);
    }

    private Conflict conflictModel(RefBookConflictEntity entity) {
        if (entity == null)
            return null;

        Conflict conflict = new Conflict();
        conflict.setRefAttributeCode(entity.getRefFieldCode());
        conflict.setConflictType(entity.getConflictType());

        RefBookRowValue referrerRowValue = getSystemRowValue(entity.getReferrerVersion(), entity.getRefRecordId());
        conflict.setPrimaryValues(getRowPrimaryValues(referrerRowValue, entity.getReferrerVersion().getStructure()));

        return conflict;
    }

    @Override
    @Transactional
    public Integer getConflictId(Integer refFromId, Integer refToId, Long rowSystemId, String refFieldCode) {

        RefBookConflictEntity entity = conflictRepository.findByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdAndRefFieldCode(refFromId, refToId, rowSystemId, refFieldCode);
        return Objects.nonNull(entity) ? entity.getId() : null;
    }

    @Override
    @Transactional
    public void handleConflict(Integer id, LocalDateTime handlingDate) {
        if (handlingDate == null)
            handlingDate = LocalDateTime.now();

        conflictRepository.setHandlingDate(id, handlingDate);
    }

    private void handleConflict(Integer refFromId, Integer refToId, Long rowSystemId, String refFieldCode, LocalDateTime handlingDate) {

        Integer id = getConflictId(refFromId, refToId, rowSystemId, refFieldCode);
        if (id != null)
            handleConflict(id, handlingDate);
    }

    /**
     * Обновление ссылок в справочнике по первичным ключам.
     *
     * @param refFromId идентификатор версии справочника со ссылками
     * @param refToId   идентификатор версии изменённого справочника
     * @param conflicts список конфликтов
     */
    @Override
    @Transactional
    public void refreshReferencesByPrimary(Integer refFromId, Integer refToId, List<Conflict> conflicts) {

        if (CollectionUtils.isEmpty(conflicts)
                || conflicts.stream().noneMatch(ConflictUtils::isUpdatedConflict))
            return;

        versionValidation.validateVersionExists(refFromId);
        versionValidation.validateVersionExists(refToId);

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        if (!isDraftEntity(refFromEntity)) {
            RefBookVersionEntity refLastEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refFromEntity.getRefBook().getCode(), RefBookVersionStatus.PUBLISHED);
            if (refLastEntity != null && !refLastEntity.getId().equals(refFromId))
                throw new RdmException(VERSION_IS_NOT_LAST_PUBLISHED);

            // NB: Изменение данных возможно только в черновике.
            Draft draft = draftService.createFromVersion(refFromId);
            // NB: Исключить, если создание конфликтов будет добавлено в код создания черновика.
            conflicts.forEach(conflict -> create(draft.getId(), refToId, conflict));
            refFromEntity = versionRepository.getOne(draft.getId());
        }

        RefBookVersionEntity refToEntity = versionRepository.getOne(refToId);

        updateReferenceValues(refFromEntity, refToEntity, conflicts);
    }

    /**
     * Обновление ссылок в справочнике по списку конфликтов.
     *
     * @param refFromEntity версия справочника со ссылками
     * @param refToEntity   версия изменённого справочника
     * @param conflicts     список конфликтов
     */
    private void updateReferenceValues(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity, List<Conflict> conflicts) {
        if (!isDraftEntity(refFromEntity))
            throw new RdmException(VERSION_IS_NOT_DRAFT);

        conflicts.stream()
                .filter(ConflictUtils::isUpdatedConflict)
                .forEach(conflict -> updateReferenceValue(refFromEntity, refToEntity, conflict));
    }

    /**
     * Обновление ссылки в справочнике по конфликту.
     *
     * @param refFromEntity версия справочника со ссылками
     * @param refToEntity   версия изменённого справочника
     * @param conflict      конфликт
     */
    private void updateReferenceValue(RefBookVersionEntity refFromEntity,
                                      RefBookVersionEntity refToEntity,
                                      Conflict conflict) {
        if (conflict == null || conflict.isEmpty())
            return;

        RefBookRowValue refFromRow = getRefFromRowValue(refFromEntity, conflict.getPrimaryValues());
        if (refFromRow == null)
            throw new NotFoundException(CONFLICTED_FROM_ROW_NOT_FOUND);

        FieldValue referenceFieldValue = refFromRow.getFieldValue(conflict.getRefAttributeCode());
        if (!(referenceFieldValue instanceof ReferenceFieldValue))
            throw new NotFoundException(CONFLICTED_REFERENCE_NOT_FOUND);

        Structure.Reference refFromReference = refFromEntity.getStructure().getReference(conflict.getRefAttributeCode());
        Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(refToEntity.getStructure());

        Reference oldReference = ((ReferenceFieldValue) referenceFieldValue).getValue();
        RefBookRowValue refToRow = getRefToRowValue(refToEntity, refToAttribute, (ReferenceFieldValue) referenceFieldValue);
        if (refToRow == null)
            throw new NotFoundException(CONFLICTED_TO_ROW_NOT_FOUND);

        String displayValue = RowUtils.toDisplayValue(refFromReference.getDisplayExpression(), refToRow);
        if (!Objects.equals(oldReference.getDisplayValue(), displayValue)) {
            Reference newReference = new Reference(
                    refToEntity.getStorageCode(),
                    refToEntity.getFromDate(), // SYS_PUBLISH_TIME is not exist for draft
                    refToAttribute.getCode(),
                    new DisplayExpression(refFromReference.getDisplayExpression()),
                    oldReference.getValue(),
                    displayValue);

            updateReferenceValue(refFromEntity.getId(),
                    refFromEntity.getStorageCode(),
                    refFromRow.getSystemId(),
                    refFromReference.getAttribute(),
                    newReference);
        }

        handleConflict(refFromEntity.getId(), refToEntity.getId(), refFromRow.getSystemId(), conflict.getRefAttributeCode(), null);
    }

    /**
     * Обновление ссылки в справочнике.
     *
     * @param refFromId          идентификатор версии справочника
     * @param refFromStorageCode код хранилища версии справочника
     * @param rowSystemId        системный идентификатор записи
     * @param referenceFieldCode название поля-ссылки
     * @param fieldReference     данные для обновления
     */
    private void updateReferenceValue(Integer refFromId, String refFromStorageCode, Long rowSystemId,
                                      String referenceFieldCode, Reference fieldReference) {
        FieldValue fieldValue = new ReferenceFieldValue(referenceFieldCode, fieldReference);
        LongRowValue rowValue = new LongRowValue(rowSystemId, singletonList(fieldValue));

        draftDataService.updateRow(refFromStorageCode, new RefBookRowValue(rowValue, refFromId));
    }

    /**
     * Обнаружение конфликтов при смене версий.
     *
     * @param oldVersionId идентификатор старой версии справочника
     * @param newVersionId идентификатор новой версии справочника
     */
    @Override
    @Transactional
    public void discoverConflicts(Integer oldVersionId, Integer newVersionId, boolean processResolvables) {

        versionValidation.validateVersionExists(oldVersionId);
        versionValidation.validateVersionExists(newVersionId);

        RefBookVersionEntity oldVersionEntity = versionRepository.getOne(oldVersionId);

        List<RefBookVersion> allReferrers = refBookService.getReferrerVersions(oldVersionEntity.getRefBook().getCode(), RefBookSourceType.ALL, null);
        if (CollectionUtils.isEmpty(allReferrers))
            return;

        List<DiffRowValue> diffRowValues = getDataDiffContent(oldVersionId, newVersionId);
        if (CollectionUtils.isEmpty(diffRowValues))
            return;

        RefBookVersionEntity newVersionEntity = versionRepository.getOne(newVersionId);

        List<Integer> referrerIds = allReferrers.stream().map(RefBookVersion::getRefBookId).distinct().collect(Collectors.toList());
        List<RefBookVersion> lastReferrers = refBookService.getReferrerVersions(oldVersionEntity.getRefBook().getCode(), RefBookSourceType.LAST_VERSION, referrerIds);
        List<Integer> lastVersionIds = lastReferrers.stream().map(RefBookVersion::getId).collect(Collectors.toList());

        allReferrers.forEach(referrer -> {
            RefBookVersionEntity referrerEntity = versionRepository.getOne(referrer.getId());
            List<Conflict> conflicts = createDiffConflicts(diffRowValues,
                    getDataAllRowContent(referrerEntity),
                    newVersionEntity.getStructure(),
                    referrer.getStructure(),
                    getRefAttributes(referrer.getStructure(), newVersionEntity.getRefBook().getCode())
            );
            if (CollectionUtils.isEmpty(conflicts))
                return;

            conflicts.forEach(conflict -> create(referrer.getId(), newVersionId, conflict));

            if (processResolvables
                    && lastVersionIds.contains(referrer.getId())
                    && conflicts.stream().anyMatch(ConflictUtils::isUpdatedConflict)) {
                refreshReferencesByPrimary(referrer.getId(), newVersionId, conflicts);
            }
        });
    }

    /**
     * Получение ссылочных атрибутов.
     *
     * @param refFromStructure структура версии справочника, которая ссылается
     * @param refToBookCode    код справочника, на который ссылаются
     * @return Список атрибутов
     */
    private List<Structure.Attribute> getRefAttributes(Structure refFromStructure, String refToBookCode) {
        return refFromStructure.getRefCodeReferences(refToBookCode)
                .stream()
                .map(ref ->
                        refFromStructure.getAttribute(ref.getAttribute()))
                .collect(toList());
    }

    /**
     * Сравнение записей данных версий справочников.
     *
     * @param oldVersionId идентификатор старой версии
     * @param newVersionId идентификатор новой версии
     * @return Список различий
     */
    private List<DiffRowValue> getDataDiffContent(Integer oldVersionId, Integer newVersionId) {
        RefBookDataDiff dataDiff = compareService.compareData(new CompareDataCriteria(oldVersionId, newVersionId));
        return dataDiff.getRows().getContent();
    }

    /**
     * Получение всех записей данных версии справочника.
     *
     * @param version версия
     * @return Список всех записей
     */
    private List<RefBookRowValue> getDataAllRowContent(RefBookVersionEntity version) {
        return getDataAllRowContent(version.getId(), version.getStorageCode(), version.getStructure(),
                version.getFromDate(), version.getToDate());
    }

    private List<RefBookRowValue> getDataAllRowContent(Integer versionId, String storageCode,
                                                       Structure structure,
                                                       LocalDateTime bdate, LocalDateTime edate) {
        DataCriteria criteria = new DataCriteria(storageCode, bdate, edate, fields(structure), null);
        criteria.setPage(0);
        criteria.setSize(0);

        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(criteria);
        if (pagedData.getCollection() == null)
            return emptyList();

        return pagedData.getCollection().stream()
                .map(rowValue -> new RefBookRowValue((LongRowValue) rowValue, versionId))
                .collect(Collectors.toList());
    }

    /**
     * Получение значений первичных ключей
     * по записи {@code rowValue} на основании структуры {@code structure}.
     *
     * @param rowValue  запись справочника
     * @param structure структура справочника
     * @return Список значений полей для первичных ключей
     */
    private List<FieldValue> getRowPrimaryValues(RefBookRowValue rowValue, Structure structure) {

        if (rowValue == null || structure == null)
            return emptyList();

        return rowValue
                .getFieldValues()
                .stream()
                .filter(fieldValue ->
                        structure.getAttribute(fieldValue.getField()).getIsPrimary())
                .collect(toList());
    }

    /**
     * Получение черновика справочника.
     *
     * @param refBookId идентификатор справочника
     * @return Черновик справочника
     */
    private RefBookVersionEntity getRefBookDraftVersion(Integer refBookId) {
        RefBookVersionEntity entity = versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookId);
        if (entity == null)
            throw new NotFoundException(new Message(REFBOOK_DRAFT_NOT_FOUND, refBookId));

        return entity;
    }

    /**
     * Получение записи по системному идентификатору.
     */
    private RefBookRowValue getSystemRowValue(RefBookVersionEntity entity, Long systemId) {

        if (entity == null || systemId == null)
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordIdFilter = new AttributeFilter("SYS_RECORDID", BigInteger.valueOf(systemId), FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordIdFilter)));

        Page<RefBookRowValue> rowValues = versionService.search(entity.getId(), criteria);
        return (rowValues != null && !CollectionUtils.isEmpty(rowValues.getContent())) ? rowValues.getContent().get(0) : null;
    }

    /**
     * Получение конфликтной записи по конфликту.
     */
    private RefBookRowValue getRefFromRowValue(RefBookVersionEntity entity, List<FieldValue> fieldValues) {

        if (entity == null || CollectionUtils.isEmpty(fieldValues))
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        fieldValues.forEach(fieldValue -> {
            FieldType fieldType = entity.getStructure().getAttribute(fieldValue.getField()).getType();
            filters.add(new AttributeFilter(fieldValue.getField(), fieldValue.getValue(), fieldType, SearchTypeEnum.EXACT));
        });
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(entity.getId(), criteria);
        return (rowValues != null && !CollectionUtils.isEmpty(rowValues.getContent())) ? rowValues.getContent().get(0) : null;
    }

    /**
     * Получение записи по ссылке из конфликтной записи.
     */
    private RefBookRowValue getRefToRowValue(RefBookVersionEntity entity, Structure.Attribute attribute, ReferenceFieldValue fieldValue) {

        if (entity == null || attribute == null || fieldValue == null)
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        Object attributeValue = castRefValue(fieldValue, attribute.getType());
        AttributeFilter filter = new AttributeFilter(attribute.getCode(), attributeValue, attribute.getType(), SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(entity.getId(), criteria);
        return (rowValues != null && !CollectionUtils.isEmpty(rowValues.getContent())) ? rowValues.getContent().get(0) : null;
    }

    /**
     * Проверка статуса версии на DRAFT.
     *
     * @param versionEntity версия
     * @return Результат проверки
     */
    private static boolean isDraftEntity(RefBookVersionEntity versionEntity) {
        return RefBookVersionStatus.DRAFT.equals(versionEntity.getStatus());
    }
}
