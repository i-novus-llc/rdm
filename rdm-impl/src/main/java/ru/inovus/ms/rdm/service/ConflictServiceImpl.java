package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.model.version.AttributeFilter;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.model.conflict.Conflict;
import ru.inovus.ms.rdm.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.model.draft.Draft;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.repositiory.RefBookConflictRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.ConflictUtils;
import ru.inovus.ms.rdm.util.RowUtils;
import ru.inovus.ms.rdm.validation.VersionValidation;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.repositiory.RefBookConflictPredicates.*;
import static ru.inovus.ms.rdm.util.ComparableUtils.*;
import static ru.inovus.ms.rdm.util.ConflictUtils.conflictTypeToDiffStatus;
import static ru.inovus.ms.rdm.util.ConflictUtils.diffStatusToConflictType;
import static ru.inovus.ms.rdm.util.ConverterUtil.field;
import static ru.inovus.ms.rdm.util.ConverterUtil.fields;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private static final String CONFLICT_REFERRER_VERSION_ID_SORT_PROPERTY = "referrerVersionId";
    private static final String CONFLICT_PUBLISHED_VERSION_ID_SORT_PROPERTY = "publishedVersionId";
    private static final String CONFLICT_REF_RECORD_ID_SORT_PROPERTY = "refRecordId";
    private static final String CONFLICT_REF_FIELD_CODE_SORT_PROPERTY = "refFieldCode";

    private static final String REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE = "refbook.draft.not.found";
    private static final String VERSION_IS_NOT_DRAFT_EXCEPTION_CODE = "version.is.not.draft";
    private static final String VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE = "version.is.not.last.published";

    private static final String REFERRER_ROW_NOT_FOUND_EXCEPTION_CODE = "referrer.row.not.found";
    private static final String CONFLICTED_TO_ROW_NOT_FOUND_EXCEPTION_CODE = "conflicted.to.row.not.found";
    private static final String CONFLICTED_REFERENCE_NOT_FOUND_EXCEPTION_CODE = "conflicted.reference.row.not.found";

    private static final String CANNOT_ORDER_BY_EXCEPTION_CODE = "cannot.order.by \"{0}\"";

    private RefBookConflictRepository conflictRepository;
    private RefBookVersionRepository versionRepository;

    private DraftDataService draftDataService;
    private SearchDataService searchDataService;

    private RefBookService refBookService;
    private VersionService versionService;
    private DraftService draftService;
    private CompareService compareService;

    private VersionValidation versionValidation;

    private EntityManager entityManager;

    @Autowired
    @SuppressWarnings("all")
    public ConflictServiceImpl(RefBookConflictRepository conflictRepository, RefBookVersionRepository versionRepository,
                               DraftDataService draftDataService, SearchDataService searchDataService,
                               RefBookService refBookService, VersionService versionService,
                               DraftService draftService, CompareService compareService,
                               VersionValidation versionValidation, EntityManager entityManager) {
        this.conflictRepository = conflictRepository;
        this.versionRepository = versionRepository;

        this.compareService = compareService;
        this.draftDataService = draftDataService;
        this.searchDataService = searchDataService;

        this.refBookService = refBookService;
        this.versionService = versionService;
        this.draftService = draftService;

        this.versionValidation = versionValidation;

        this.entityManager = entityManager;
    }

    /**
     * Вычисление конфликтов справочников при наличии ссылочных атрибутов.
     *
     * @param refFromId идентификатор версии, которая ссылается
     * @param refToId   идентификатор последней опубликованной версии
     *                  (идентификатор публикуемого черновика определяется автоматически)
     * @return Список конфликтов для версии, которая ссылается
     *
     * @see #checkConflicts
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

        List<DiffRowValue> diffRowValues = getDataDiffContent(oldRefToId, newRefToId);

        return calculateDiffConflicts(refFromEntity, refToEntity, diffRowValues);
    }

    /**
     * Вычисление конфликтов справочников по diff-записям.
     *
     * @param refFromEntity версия, которая ссылается
     * @param refToEntity   версия, на которую ссылаются
     * @param diffRowValues diff-записи
     * @return Список конфликтов для версии, которая ссылается
     *
     * @see #checkDiffConflicts
     */
    private List<Conflict> calculateDiffConflicts(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                                  List<DiffRowValue> diffRowValues) {
        List<Structure.Attribute> refFromAttributes = getRefAttributes(refFromEntity.getStructure(),
                refToEntity.getRefBook().getCode());
        List<RefBookRowValue> refFromRowValues = getConflictedRowContent(refFromEntity, diffRowValues,
                refToEntity.getStructure().getPrimary(), refFromAttributes);

        return refFromAttributes
                .stream()
                .flatMap(refFromAttribute ->
                        diffRowValues
                                .stream()
                                .flatMap(diffRowValue -> {
                                    List<RefBookRowValue> rowValues =
                                            findRefBookRowValues(refToEntity.getStructure().getPrimary(), refFromAttribute,
                                                    diffRowValue, refFromRowValues);
                                    return rowValues.stream()
                                            .map(rowValue ->
                                                    createDiffConflict(diffRowValue, rowValue, refFromAttribute, refFromEntity.getStructure()));
                                })
                ).collect(toList());
    }

    /**
     * Создание записи о diff-конфликте.
     *
     * @param diffRowValue     diff-запись
     * @param refFromRowValue  запись из версии, которая ссылается
     * @param refFromAttribute ссылочный атрибут версии, которая ссылается
     * @param refFromStructure структура версии, которая ссылается
     * @return Запись о diff-конфликте
     */
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
     * @param refFromId идентификатор версии, которая ссылается
     * @param refToId   идентификатор публикуемой версии
     *                  (идентификатор последней опубликованной версии определяется автоматически)
     * @return Наличие конфликтов для версии, которая ссылается
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
     * @return Наличие конфликтов для версии, которая ссылается
     */
    private Boolean checkConflicts(Integer refFromId, Integer oldRefToId, Integer newRefToId, ConflictType conflictType) {

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity refToEntity = versionRepository.getOne(oldRefToId);

        List<DiffRowValue> diffRowValues = getDataDiffContent(oldRefToId, newRefToId);

        return checkDiffConflicts(refFromEntity, refToEntity, diffRowValues, conflictTypeToDiffStatus(conflictType));
    }

    /**
     * Проверка на наличие конфликтов справочников по diff-записям.
     *
     * @param refFromEntity версия, которая ссылается
     * @param refToEntity   версия, на которую ссылаются
     * @param diffRowValues diff-записи
     * @param diffStatus    статус diff-записи
     * @return Наличие конфликтов для версии, которая ссылается
     *
     * @see #calculateDiffConflicts
     */
    private Boolean checkDiffConflicts(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                       List<DiffRowValue> diffRowValues, DiffStatusEnum diffStatus) {
        List<Structure.Attribute> refFromAttributes = getRefAttributes(refFromEntity.getStructure(),
                refToEntity.getRefBook().getCode());
        List<RefBookRowValue> refFromRowValues = getConflictedRowContent(refFromEntity, diffRowValues,
                refToEntity.getStructure().getPrimary(), refFromAttributes);

        return refFromAttributes
                .stream()
                .anyMatch(refFromAttribute ->
                        diffRowValues
                                .stream()
                                .filter(diffRowValue ->
                                        diffStatus.equals(diffRowValue.getStatus()))
                                .anyMatch(diffRowValue -> {
                                    RefBookRowValue rowValue =
                                            findRefBookRowValue(refToEntity.getStructure().getPrimary(), refFromAttribute,
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
     * Обновление ссылок в справочниках по первичным ключам.
     *
     * @param oldVersionId идентификатор старой версии справочника
     * @param newVersionId идентификатор новой версии справочника
     *
     * @see #discoverConflicts(Integer, Integer)
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

        lastReferrers.forEach(referrer -> {
            List<Conflict> conflicts = calculateConflicts(referrer.getId(), oldVersionId, newVersionId);
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

        RefBookVersionEntity refFromEntity = getOrCreateDraftEntity(refFromId);
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
     * Поиск конфликтов по критерию поиска.
     *
     * @param criteria критерий поиска
     * @return Страница конфликтов
     */
    @Override
    @Transactional
    public Page<RefBookConflict> search(RefBookConflictCriteria criteria) {
        JPAQuery<RefBookConflictEntity> jpaQuery =
                new JPAQuery<>(entityManager)
                        .select(QRefBookConflictEntity.refBookConflictEntity)
                        .from(QRefBookConflictEntity.refBookConflictEntity)
                        .where(toPredicate(criteria));

        long count = jpaQuery.fetchCount();

        sortQuery(jpaQuery, criteria);
        List<RefBookConflictEntity> refBookVersionEntityList = jpaQuery
                .offset(criteria.getOffset())
                .limit(criteria.getPageSize())
                .fetch();

        Page<RefBookConflictEntity> list = new PageImpl<>(refBookVersionEntityList, criteria, count);
        return list.map(this::refBookConflictModel);
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

    private RefBookConflictEntity createRefBookConflictEntity(Integer refFromId, Integer refToId, Conflict conflict) {

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity refToEntity = versionRepository.getOne(refToId);

        RefBookRowValue refFromRowValue = getRefFromRowValue(refFromEntity, conflict.getPrimaryValues());
        if (refFromRowValue == null)
            throw new NotFoundException(REFERRER_ROW_NOT_FOUND_EXCEPTION_CODE);

        return createRefBookConflictEntity(refFromEntity, refToEntity, refFromRowValue.getSystemId(), conflict);
    }

    private RefBookConflictEntity createRefBookConflictEntity(RefBookVersionEntity referrerEntity, RefBookVersionEntity publishedEntity,
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
     * Получение всех конфликтов для версии, которая ссылается,
     * с любыми справочниками по указанным записям.
     *
     * @param referrerVersionId идентификатор версии справочника, который ссылается
     * @param refRecordIds      список системных идентификаторов записей версии
     * @return Список конфликтов
     */
    @Override
    public List<RefBookConflict> getReferrerConflicts(Integer referrerVersionId, List<Long> refRecordIds) {

        versionValidation.validateVersionExists(referrerVersionId);

        List<RefBookConflictEntity> refBookConflicts =
                isEmpty(refRecordIds)
                        ? conflictRepository.findAllByReferrerVersionId(referrerVersionId)
                        : conflictRepository.findAllByReferrerVersionIdAndRefRecordIdIn(referrerVersionId, refRecordIds);

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
     * Обновление ссылок в справочнике по таблице конфликтов.
     *
     * @param referrerVersionId идентификатор версии справочника
     */
    @Override
    @Transactional
    public void refreshReferrerByPrimary(Integer referrerVersionId) {

        versionValidation.validateVersionExists(referrerVersionId);

        RefBookVersionEntity referrerEntity = getOrCreateDraftEntity(referrerVersionId);
        List<Structure.Reference> references = referrerEntity.getStructure().getReferences();
        if (isEmpty(references))
            return;

        references.forEach(reference -> {
            List<RefBookConflictEntity> conflicts =
                    conflictRepository.findAllByReferrerVersionIdAndRefFieldCodeAndConflictType(referrerVersionId, reference.getAttribute(), ConflictType.UPDATED);

            List<RefBookVersionEntity> publishedVersions = conflicts.stream()
                    .map(RefBookConflictEntity::getPublishedVersion)
                    .distinct()
                    .collect(toList());

            publishedVersions.forEach(publishedVersion -> {
                Structure.Attribute refToAttribute = reference.findReferenceAttribute(publishedVersion.getStructure());
                Reference updatedReference = new Reference(
                        publishedVersion.getStorageCode(),
                        publishedVersion.getFromDate(), // SYS_PUBLISH_TIME is not exist for draft
                        refToAttribute.getCode(),
                        new DisplayExpression(reference.getDisplayExpression()),
                        null, // NB: Old value is not changed
                        null // NB: Display value will be recalculated
                );
                ReferenceFieldValue fieldValue = new ReferenceFieldValue(reference.getAttribute(), updatedReference);

                List<RefBookConflictEntity> updatedConflicts = conflicts.stream()
                        .filter(conflict -> conflict.getPublishedVersion() == publishedVersion)
                        .collect(toList());

                List<Object> systemIds = updatedConflicts.stream()
                        .map(RefBookConflictEntity::getRefRecordId)
                        .collect(toList());

                draftDataService.updateReferenceInRows(referrerEntity.getStorageCode(), fieldValue, systemIds);

                conflictRepository.deleteInBatch(updatedConflicts);
            });
        });
    }

    /**
     * Обновление ссылок в свяазанных справочниках по таблице конфликтов.
     *
     * @param refBookCode код справочника, на который ссылаются
     */
    @Override
    @Transactional
    public void refreshLastReferrersByPrimary(String refBookCode) {
        List<RefBookVersion> lastReferrers = refBookService.getReferrerVersions(refBookCode, RefBookSourceType.LAST_VERSION, null);
        lastReferrers.forEach(referrer -> refreshReferrerByPrimary(referrer.getId()));
    }

    /**
     * Удаление конфиктов со всеми версиями справочника, на который ссылаются.
     *
     * @param publishedRefBookId идентификатор справочника, на который ссылаются
     */
    @Transactional
    public void dropPublishedConflicts(Integer publishedRefBookId) {
        conflictRepository.deleteByPublishedVersionRefBookId(publishedRefBookId);
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

        allReferrers.forEach(referrer -> {
            List<Conflict> conflicts = calculateConflicts(referrer.getId(), oldVersionId, newVersionId);
            if (isEmpty(conflicts))
                return;

            conflicts.forEach(conflict -> create(referrer.getId(), newVersionId, conflict));
        });
    }

    /**
     * Копирование конфликтов при смене версий справочника.
     *
     * @param oldVersionId идентификатор старой версии справочника
     * @param newVersionId идентификатор новой версии справочника
     */
    @Override
    @Transactional
    public void copyConflicts(Integer oldVersionId, Integer newVersionId) {

        versionValidation.validateVersionExists(oldVersionId);
        versionValidation.validateVersionExists(newVersionId);

        if (!newVersionId.equals(oldVersionId))
            conflictRepository.copyByReferrerVersion(oldVersionId, newVersionId);
    }

    /**
     * Формирование предиката на основе критерия.
     *
     * @param criteria критерий
     * @return Предикат
     */
    private Predicate toPredicate(RefBookConflictCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        if (nonNull(criteria.getReferrerVersionId()))
            where.and(isReferrerVersionId(criteria.getReferrerVersionId()));

        if (nonNull(criteria.getReferrerVersionRefBookId()))
            where.and(isReferrerVersionRefBookId(criteria.getReferrerVersionRefBookId()));

        if (nonNull(criteria.getPublishedVersionId()))
            where.and(isPublishedVersionId(criteria.getPublishedVersionId()));

        if (nonNull(criteria.getPublishedVersionRefBookId()))
            where.and(isPublishedVersionRefBookId(criteria.getPublishedVersionRefBookId()));

        if (nonNull(criteria.getRefRecordId()))
            where.and(isRefRecordId(criteria.getRefRecordId()));

        if (nonNull(criteria.getRefFieldCode()))
            where.and(isRefFieldCode(criteria.getRefFieldCode()));

        if (nonNull(criteria.getConflictType()))
            where.and(isConflictType(criteria.getConflictType()));

        return where.getValue();
    }

    /**
     * Добавление сортировки в запрос на основе критерия.
     *
     * @param jpaQuery запрос
     * @param criteria критерий
     */
    private void sortQuery(JPAQuery<RefBookConflictEntity> jpaQuery, RefBookConflictCriteria criteria) {

        List<Sort.Order> orders = criteria.getOrders();

        if (!CollectionUtils.isEmpty(orders)) {
            criteria.getOrders().stream()
                    .filter(Objects::nonNull)
                    .forEach(order -> addSortOrder(jpaQuery, order));
        }
    }

    /**
     * Добавление сортировки в запрос по заданному порядку.
     *
     * @param jpaQuery запрос
     * @param order    порядок
     */
    private void addSortOrder(JPAQuery<RefBookConflictEntity> jpaQuery, Sort.Order order) {

        ComparableExpressionBase sortExpression;

        switch (order.getProperty()) {
            case CONFLICT_REFERRER_VERSION_ID_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.referrerVersion.id;
                break;

            case CONFLICT_PUBLISHED_VERSION_ID_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.publishedVersion.id;
                break;

            case CONFLICT_REF_RECORD_ID_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.refRecordId;
                break;

            case CONFLICT_REF_FIELD_CODE_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.refFieldCode;
                break;

            default:
                throw new UserException(new Message(CANNOT_ORDER_BY_EXCEPTION_CODE, order.getProperty()));
        }

        jpaQuery.orderBy(order.isAscending() ? sortExpression.asc() : sortExpression.desc());
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
     * Список записей о сравнении фильтруется по статусу изменения: остаются только DELETED, UPDATED
     *
     * @param oldVersionId идентификатор старой версии
     * @param newVersionId идентификатор новой версии
     * @return Список различий
     */
    private List<DiffRowValue> getDataDiffContent(Integer oldVersionId, Integer newVersionId) {
        return compareService.compareData(new CompareDataCriteria(oldVersionId, newVersionId))
                .getRows()
                .getContent()
                .stream()
                .filter(diffRowValue ->
                        asList(DiffStatusEnum.DELETED, DiffStatusEnum.UPDATED)
                                .contains(diffRowValue.getStatus()))
                .collect(toList());
    }

    /**
     * Получение записей данных версии справочника для diff-записей.
     *
     * @param refFromVersion    версия справочника, который ссылается
     * @param diffRowValues     diff-записи
     * @param refToPrimaries    первичные ключи справочника, на который ссылаются
     * @param refFromAttributes ссылочные атрибуты версии, которая ссылается
     * @return Список всех записей
     */
    private List<RefBookRowValue> getConflictedRowContent(RefBookVersionEntity refFromVersion, List<DiffRowValue> diffRowValues,
                                                          List<Structure.Attribute> refToPrimaries, List<Structure.Attribute> refFromAttributes) {
        Set<List<FieldSearchCriteria>> filters = createFiltersForDiffRowValues(diffRowValues, refToPrimaries, refFromAttributes);
        return getConflictedRowContent(refFromVersion.getId(), refFromVersion.getStorageCode(), refFromVersion.getStructure(),
                refFromVersion.getFromDate(), refFromVersion.getToDate(), filters);
    }

    private List<RefBookRowValue> getConflictedRowContent(Integer versionId, String storageCode,
                                                          Structure structure,
                                                          LocalDateTime bdate, LocalDateTime edate,
                                                          Set<List<FieldSearchCriteria>> filters) {
        DataCriteria criteria = new DataCriteria(storageCode, bdate, edate, fields(structure), filters, null);
        // NB: Get all required rows.
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
        return diffRowValues
                .stream()
                .flatMap(diff -> {
                    DiffFieldValue diffFieldValue = diff.getDiffFieldValue(refToPrimaries.get(0).getCode());
                    Object value = DiffStatusEnum.DELETED.equals(diff.getStatus())
                            ? diffFieldValue.getOldValue()
                            : diffFieldValue.getNewValue();
                    return refFromAttributes
                            .stream()
                            .map(refFromAttribute ->
                                    singletonList(new FieldSearchCriteria(field(refFromAttribute), SearchTypeEnum.EXACT, singletonList(value)))
                            );
                }).collect(toSet());
    }

    /**
     * Получение или создание entity версии-черновика справочника.
     *
     * @param versionId версия справочника
     * @return Entity версии-черновика справочника
     */
    private RefBookVersionEntity getOrCreateDraftEntity(Integer versionId) {

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        if (versionEntity.isDraft())
            return versionEntity;

        RefBookVersionEntity refLastEntity =
                versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                        versionEntity.getRefBook().getCode(),
                        RefBookVersionStatus.PUBLISHED
                );
        if (refLastEntity != null && !refLastEntity.getId().equals(versionId))
            throw new RdmException(VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE);

        // NB: Изменение данных возможно только в черновике.
        Draft draft = draftService.createFromVersion(versionId);
        return versionRepository.getOne(draft.getId());
    }
}
