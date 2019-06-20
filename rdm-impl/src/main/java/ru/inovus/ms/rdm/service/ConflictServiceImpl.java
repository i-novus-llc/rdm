package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.ComparableUtils.*;
import static ru.inovus.ms.rdm.util.ConflictUtils.conflictTypeToDiffStatus;
import static ru.inovus.ms.rdm.util.ConflictUtils.diffStatusToConflictType;
import static ru.inovus.ms.rdm.util.ConverterUtil.fields;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private static final String REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE = "refbook.draft.not.found";
    private static final String VERSION_IS_NOT_DRAFT_EXCEPTION_CODE = "version.is.not.draft";
    private static final String VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE = "version.is.not.last.published";
    private static final String REFERRER_ROW_NOT_FOUND_EXCEPTION_CODE = "referrer.row.not.found";
    private static final String CONFLICTED_TO_ROW_NOT_FOUND_EXCEPTION_CODE = "conflicted.to.row.not.found";
    private static final String CONFLICTED_REFERENCE_NOT_FOUND_EXCEPTION_CODE = "conflicted.reference.row.not.found";

    private RefBookConflictRepository conflictRepository;
    private RefBookVersionRepository versionRepository;

    private CompareService compareService;
    private DraftDataService draftDataService;
    private SearchDataService searchDataService;

    private RefBookService refBookService;
    private VersionService versionService;
    private DraftService draftService;

    private VersionValidation versionValidation;

    @Autowired
    @SuppressWarnings("all")
    public ConflictServiceImpl(RefBookConflictRepository conflictRepository,
                               RefBookVersionRepository versionRepository,
                               CompareService compareService,
                               DraftDataService draftDataService,
                               SearchDataService searchDataService,
                               RefBookService refBookService,
                               VersionService versionService,
                               DraftService draftService,
                               VersionValidation versionValidation) {
        this.conflictRepository = conflictRepository;
        this.versionRepository = versionRepository;

        this.compareService = compareService;
        this.draftDataService = draftDataService;
        this.searchDataService = searchDataService;

        this.refBookService = refBookService;
        this.versionService = versionService;
        this.draftService = draftService;

        this.versionValidation = versionValidation;
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

        return calculateDiffConflicts(getDataDiffContent(oldRefToId, newRefToId),
                getDataAllRowContent(refFromEntity),
                refToEntity.getStructure(),
                refFromEntity.getStructure(),
                getRefAttributes(refFromEntity.getStructure(), refToEntity.getRefBook().getCode())
        );
    }

    private List<Conflict> calculateDiffConflicts(List<DiffRowValue> diffRowValues, List<RefBookRowValue> refFromRowValues,
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
                                                    calculateDiffConflict(diffRowValue, rowValue, refFromAttribute, refFromStructure));
                                })
                ).collect(toList());
    }

    private Conflict calculateDiffConflict(DiffRowValue diffRowValue, RefBookRowValue refFromRowValue,
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
     * Получение справочников, имеющих конфликты
     * с неопубликованной версией проверяемого справочника.
     *
     * @param versionId    идентификатор неопубликованной версии справочника
     * @param conflictType тип конфликта
     * @return Список справочников
     */
    @Override
    @Transactional(readOnly = true)
    public List<RefBookVersion> getCheckConflictReferrers(Integer versionId, ConflictType conflictType) {

        versionValidation.validateVersionExists(versionId);

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        List<RefBookVersion> referrers = refBookService.getReferrerVersions(versionEntity.getRefBook().getCode(), RefBookSourceType.LAST_VERSION, null);
        return referrers.stream()
                .filter(referrer -> checkConflicts(referrer.getId(), versionId, conflictType))
                .collect(toList());
    }

    /**
     * Сохранение информации о конфликте.
     *
     * @param refFromId идентификатор черновика справочника со ссылками
     * @param refToId   идентификатор версии изменённого справочника
     * @param conflict  конфликт
     */
    @Override
    @Transactional
    public void create(Integer refFromId, Integer refToId, Conflict conflict) {
        if (conflict == null || conflict.isEmpty())
            return;

        RefBookConflictEntity entity = createRefBookConflictEntity(refFromId, refToId, conflict);
        conflictRepository.save(entity);
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

        List<RefBookConflictEntity> entities = conflicts.stream()
                .map(conflict -> createRefBookConflictEntity(refFromId, refToId, conflict))
                .collect(toList());

        conflictRepository.saveAll(entities);
    }

    private RefBookConflictEntity createRefBookConflictEntity (Integer refFromId, Integer refToId, Conflict conflict) {

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity refToEntity = versionRepository.getOne(refToId);

        RefBookRowValue refFromRowValue = getRefFromRowValue(refFromEntity, conflict.getPrimaryValues());
        if (refFromRowValue == null)
            throw new NotFoundException(REFERRER_ROW_NOT_FOUND_EXCEPTION_CODE);

        return createRefBookConflictEntity(refFromEntity, refToEntity, refFromRowValue.getSystemId(), conflict);
    }

    private RefBookConflictEntity createRefBookConflictEntity (RefBookVersionEntity referrerEntity, RefBookVersionEntity publishedEntity,
                                                               Long referrerRowSystemId, Conflict conflict) {
        RefBookConflictEntity entity = new RefBookConflictEntity();
        entity.setReferrerVersion(referrerEntity);
        entity.setPublishedVersion(publishedEntity);

        entity.setRefRecordId(referrerRowSystemId);
        entity.setRefFieldCode(conflict.getRefAttributeCode());
        entity.setConflictType(conflict.getConflictType());

        return entity;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        conflictRepository.deleteById(id);
    }

    private void delete(Integer refFromId, Integer refToId, Long rowSystemId, String refFieldCode) {

        Integer id = findId(refFromId, refToId, rowSystemId, refFieldCode);
        if (id != null)
            delete(id);
    }

    @Override
    public RefBookConflict find(Integer refFromId, Integer refToId, Long rowSystemId, String refFieldCode) {
        RefBookConflictEntity entity = findEntity(refFromId, refToId, rowSystemId, refFieldCode);
        return Objects.nonNull(entity) ? refBookConflictModel(entity) : null;
    }

    @Override
    public Integer findId(Integer refFromId, Integer refToId, Long rowSystemId, String refFieldCode) {
        RefBookConflictEntity entity = findEntity(refFromId, refToId, rowSystemId, refFieldCode);
        return Objects.nonNull(entity) ? entity.getId() : null;
    }

    private RefBookConflictEntity findEntity(Integer refFromId, Integer refToId, Long rowSystemId, String refFieldCode) {
        return conflictRepository.findByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdAndRefFieldCode(refFromId, refToId, rowSystemId, refFieldCode);
    }

    /**
     * Получение конфликтов для версии, которая ссылается,
     * с любыми справочниками по указанным записям.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     * @param refRecordIds      список системных идентификаторов записей версии
     * @return Список конфликтов
     */
    @Override
    public List<RefBookConflict> getConflicts(Integer referrerVersionId, List<Long> refRecordIds) {

        versionValidation.validateVersionExists(referrerVersionId);

        List<RefBookConflictEntity> refBookConflicts =
                conflictRepository.findAllByReferrerVersionIdAndRefRecordIdIn(referrerVersionId, refRecordIds);
        return refBookConflicts
                .stream()
                .map(this::refBookConflictModel)
                .collect(toList());
    }

    /**
     * Проверка версии справочника, который ссылается,
     * на наличие конфликта любого типа с любым справочником.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     * @return Наличие конфликта
     */
    @Override
    public boolean hasConflict(Integer referrerVersionId) {
        return conflictRepository.existsByReferrerVersionId(referrerVersionId);
    }

    /**
     * Проверка версии справочника, который ссылается,
     * на наличие конфликта обновления записи с любым справочником.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     * @param conflictType      тип конфликта
     * @return Наличие конфликта
     */
    @Override
    public boolean hasTypedConflict(Integer referrerVersionId, ConflictType conflictType) {
        return conflictRepository.existsByReferrerVersionIdAndConflictType(referrerVersionId, conflictType);
    }

    /**
     * Проверка версии справочника, на который ссылаются,
     * на наличие конфликта любого типа в любых справочниках.
     *
     * @param publishedVersionId идентификатор версии справочника, на который ссылаются
     * @return Наличие конфликта
     */
    @Override
    public boolean isConflicted(Integer publishedVersionId) {
        return conflictRepository.existsByPublishedVersionId(publishedVersionId);
    }

    /**
     * Обновление ссылок в справочниках по первичным ключам.
     *
     * @param oldVersionId идентификатор старой версии справочника
     * @param newVersionId идентификатор новой версии справочника
     * @see #discoverConflicts(Integer, Integer, boolean)
     */
    @Override
    @Transactional
    public void refreshReferencesByPrimary(Integer oldVersionId, Integer newVersionId) {

        versionValidation.validateVersionExists(oldVersionId);
        versionValidation.validateVersionExists(newVersionId);

        RefBookVersionEntity oldVersionEntity = versionRepository.getOne(oldVersionId);

        List<RefBookVersion> lastReferrers = refBookService.getReferrerVersions(oldVersionEntity.getRefBook().getCode(), RefBookSourceType.LAST_VERSION, null);
        if (isEmpty(lastReferrers))
            return;

        List<DiffRowValue> diffRowValues = getDataDiffContent(oldVersionId, newVersionId);
        if (isEmpty(diffRowValues))
            return;

        RefBookVersionEntity newVersionEntity = versionRepository.getOne(newVersionId);

        lastReferrers.forEach(referrer -> {
            RefBookVersionEntity referrerEntity = versionRepository.getOne(referrer.getId());
            List<Conflict> conflicts = calculateDiffConflicts(diffRowValues,
                    getDataAllRowContent(referrerEntity),
                    newVersionEntity.getStructure(),
                    referrer.getStructure(),
                    getRefAttributes(referrer.getStructure(), newVersionEntity.getRefBook().getCode())
            );
            if (isEmpty(conflicts))
                return;

            refreshReferencesByPrimary(referrer.getId(), newVersionId, conflicts);
        });
    }

    /**
     * Обновление ссылок в версии справочника по первичным ключам.
     *
     * @param refFromId идентификатор версии справочника со ссылками
     * @param refToId   идентификатор версии изменённого справочника
     * @param conflicts список конфликтов
     */
    @Override
    @Transactional
    public void refreshReferencesByPrimary(Integer refFromId, Integer refToId, List<Conflict> conflicts) {

        if (isEmpty(conflicts)
                || conflicts.stream().noneMatch(ConflictUtils::isUpdatedConflict))
            return;

        versionValidation.validateVersionExists(refFromId);
        versionValidation.validateVersionExists(refToId);

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        if (!refFromEntity.isDraft()) {
            RefBookVersionEntity refLastEntity =
                    versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                            refFromEntity.getRefBook().getCode(),
                            RefBookVersionStatus.PUBLISHED
                    );
            if (refLastEntity != null && !refLastEntity.getId().equals(refFromId))
                throw new RdmException(VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE);

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
    private void updateReferenceValues(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                       List<Conflict> conflicts) {
        if (!refFromEntity.isDraft())
            throw new RdmException(VERSION_IS_NOT_DRAFT_EXCEPTION_CODE);

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
            throw new NotFoundException(REFERRER_ROW_NOT_FOUND_EXCEPTION_CODE);

        FieldValue referenceFieldValue = refFromRow.getFieldValue(conflict.getRefAttributeCode());
        if (!(referenceFieldValue instanceof ReferenceFieldValue))
            throw new NotFoundException(CONFLICTED_REFERENCE_NOT_FOUND_EXCEPTION_CODE);

        Structure.Reference refFromReference = refFromEntity.getStructure().getReference(conflict.getRefAttributeCode());
        Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(refToEntity.getStructure());

        Reference oldReference = ((ReferenceFieldValue) referenceFieldValue).getValue();
        RefBookRowValue refToRow = getRefToRowValue(refToEntity, refToAttribute, (ReferenceFieldValue) referenceFieldValue);
        if (refToRow == null)
            throw new NotFoundException(CONFLICTED_TO_ROW_NOT_FOUND_EXCEPTION_CODE);

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

        delete(refFromEntity.getId(), refToEntity.getId(), refFromRow.getSystemId(), conflict.getRefAttributeCode());
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

        List<RefBookVersion> allReferrers = refBookService.getReferrerVersions(oldVersionEntity.getRefBook().getCode(),
                RefBookSourceType.ALL, null);
        if (isEmpty(allReferrers))
            return;

        List<DiffRowValue> diffRowValues = getDataDiffContent(oldVersionId, newVersionId);
        if (isEmpty(diffRowValues))
            return;

        RefBookVersionEntity newVersionEntity = versionRepository.getOne(newVersionId);

        List<Integer> referrerIds = allReferrers.stream().map(RefBookVersion::getRefBookId).distinct().collect(toList());
        List<RefBookVersion> lastReferrers = refBookService.getReferrerVersions(oldVersionEntity.getRefBook().getCode(),
                RefBookSourceType.LAST_VERSION, referrerIds);
        List<Integer> lastVersionIds = lastReferrers.stream().map(RefBookVersion::getId).collect(toList());

        allReferrers.forEach(referrer -> {
            RefBookVersionEntity referrerEntity = versionRepository.getOne(referrer.getId());
            List<Conflict> conflicts = calculateDiffConflicts(diffRowValues,
                    getDataAllRowContent(referrerEntity),
                    newVersionEntity.getStructure(),
                    referrer.getStructure(),
                    getRefAttributes(referrer.getStructure(), newVersionEntity.getRefBook().getCode())
            );
            if (isEmpty(conflicts))
                return;

            conflicts.forEach(conflict -> create(referrer.getId(), newVersionId, conflict));

            if (processResolvables
                    && lastVersionIds.contains(referrer.getId())
                    && conflicts.stream().anyMatch(ConflictUtils::isUpdatedConflict)) {
                refreshReferencesByPrimary(referrer.getId(), newVersionId, conflicts);
            }
        });
    }

    private RefBookConflict refBookConflictModel(RefBookConflictEntity entity) {
        if (entity == null)
            return null;

        return new RefBookConflict(entity.getReferrerVersion().getId(),
                entity.getPublishedVersion().getId(),
                entity.getRefRecordId(),
                entity.getRefFieldCode(),
                entity.getConflictType(),
                entity.getCreationDate()
        );
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
                .collect(toList());
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
            throw new NotFoundException(new Message(REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE, refBookId));

        return entity;
    }

    /**
     * Получение конфликтной записи по конфликту.
     */
    private RefBookRowValue getRefFromRowValue(RefBookVersionEntity versionEntity, List<FieldValue> fieldValues) {

        if (versionEntity == null || isEmpty(fieldValues))
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        fieldValues.forEach(fieldValue -> {
            FieldType fieldType = versionEntity.getStructure().getAttribute(fieldValue.getField()).getType();
            filters.add(new AttributeFilter(fieldValue.getField(), fieldValue.getValue(), fieldType, SearchTypeEnum.EXACT));
        });
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(versionEntity.getId(), criteria);
        return (rowValues != null && !isEmpty(rowValues.getContent()))
                ? rowValues.getContent().get(0)
                : null;
    }

    /**
     * Получение записи по ссылке из конфликтной записи.
     */
    private RefBookRowValue getRefToRowValue(RefBookVersionEntity versionEntity, Structure.Attribute attribute, ReferenceFieldValue fieldValue) {

        if (versionEntity == null || attribute == null || fieldValue == null)
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        Object attributeValue = castRefValue(fieldValue, attribute.getType());
        AttributeFilter filter = new AttributeFilter(attribute.getCode(), attributeValue, attribute.getType(), SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(versionEntity.getId(), criteria);
        return (rowValues != null && !isEmpty(rowValues.getContent()))
                ? rowValues.getContent().get(0)
                : null;
    }
}
