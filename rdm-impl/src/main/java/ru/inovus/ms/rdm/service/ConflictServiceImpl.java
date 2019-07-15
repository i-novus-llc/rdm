package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import ru.inovus.ms.rdm.enumeration.RefBookStatusType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.conflict.*;
import ru.inovus.ms.rdm.model.diff.StructureDiff;
import ru.inovus.ms.rdm.model.field.ReferenceFilterValue;
import ru.inovus.ms.rdm.model.version.AttributeFilter;
import ru.inovus.ms.rdm.model.compare.CompareDataCriteria;
import ru.inovus.ms.rdm.model.draft.Draft;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.model.version.ReferrerVersionCriteria;
import ru.inovus.ms.rdm.predicate.DeleteRefBookConflictPredicateProducer;
import ru.inovus.ms.rdm.predicate.RefBookConflictPredicateProducer;
import ru.inovus.ms.rdm.repositiory.RefBookConflictRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.*;
import ru.inovus.ms.rdm.util.PageIterator;
import ru.inovus.ms.rdm.validation.VersionValidation;

import javax.persistence.EntityManager;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.n2oapp.platform.jaxrs.RestCriteria.FIRST_PAGE_NUMBER;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.isSourceType;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.isVersionOfRefBooks;
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
    private static final int REF_BOOK_VERSION_DATA_PAGE_SIZE = 100;

    private static final String VERSION_ID_SORT_PROPERTY = "id";

    private static final int REF_BOOK_CONFLICT_PAGE_SIZE = 100;
    private static final int REF_BOOK_DIFF_CONFLICT_PAGE_SIZE = 100;

    private static final String CONFLICT_REFERRER_VERSION_ID_SORT_PROPERTY = "referrerVersionId";
    private static final String CONFLICT_PUBLISHED_VERSION_ID_SORT_PROPERTY = "publishedVersionId";
    private static final String CONFLICT_REF_RECORD_ID_SORT_PROPERTY = "refRecordId";
    private static final String CONFLICT_REF_FIELD_CODE_SORT_PROPERTY = "refFieldCode";

    private static final List<DiffStatusEnum> CALCULATING_DIFF_STATUS_LIST = asList(DiffStatusEnum.DELETED, DiffStatusEnum.UPDATED);

    private static final List<Sort.Order> SORT_REFERRER_VERSIONS = singletonList(
            new Sort.Order(Sort.Direction.ASC, VERSION_ID_SORT_PROPERTY)
    );

    private static final List<Sort.Order> SORT_VERSION_DATA = singletonList(
            new Sort.Order(Sort.Direction.ASC, DataConstants.SYS_PRIMARY_COLUMN)
    );

    private static final List<Sort.Order> SORT_REF_BOOK_CONFLICTS = asList(
            new Sort.Order(Sort.Direction.ASC, CONFLICT_REF_RECORD_ID_SORT_PROPERTY),
            new Sort.Order(Sort.Direction.ASC, CONFLICT_REF_FIELD_CODE_SORT_PROPERTY)
    );

    private static final String VERSION_IS_NOT_LAST_PUBLISHED_EXCEPTION_CODE = "version.is.not.last.published";

    private static final String REFERRER_ROW_NOT_FOUND_EXCEPTION_CODE = "referrer.row.not.found";

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
    @SuppressWarnings("squid:S00107")
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
     * Вычисление конфликтов справочников по критерию.
     *
     * @param criteria критерий вычисления
     * @return Список конфликтов
     */
    @Override
    @Transactional(readOnly = true)
    public List<Conflict> calculateDataConflicts(CalculateConflictCriteria criteria) {

        List<Conflict> list = new ArrayList<>();

        CompareDataCriteria dataCriteria = new CompareDataCriteria(criteria.getOldVersionId(), criteria.getNewVersionId());
        dataCriteria.setOrders(SORT_VERSION_DATA);
        dataCriteria.startPageNumber(FIRST_PAGE_NUMBER, REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

        RefBookVersionEntity referrerVersionEntity = versionRepository.getOne(criteria.getReferrerVersionId());
        RefBookVersionEntity oldVersionEntity = versionRepository.getOne(criteria.getOldVersionId());

        Function<CompareDataCriteria, Page<DiffRowValue>> pageSource = pageCriteria -> compareService.compareData(pageCriteria).getRows();
        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(pageSource, dataCriteria);
        pageIterator.forEachRemaining(page -> {
            List<Conflict> conflicts = calculateDataDiffConflicts(referrerVersionEntity, oldVersionEntity, getDataDiffContent(page, criteria.getStructureAltered()));
            list.addAll(conflicts);
        });

        return list;
    }

    /**
     * Вычисление конфликтов справочников по diff-записям.
     *
     * @param refFromEntity версия, которая ссылается
     * @param refToEntity   версия, на которую ссылаются
     * @param diffRowValues diff-записи
     * @return Список конфликтов для версии, которая ссылается
     * @see #checkDataDiffConflicts
     */
    private List<Conflict> calculateDataDiffConflicts(RefBookVersionEntity refFromEntity,
                                                      RefBookVersionEntity refToEntity,
                                                      List<DiffRowValue> diffRowValues) {

        List<Structure.Attribute> refToPrimaries = refToEntity.getStructure().getPrimary();
        List<Structure.Attribute> refFromAttributes = refFromEntity.getStructure().getRefCodeAttributes(refToEntity.getRefBook().getCode());
        List<RefBookRowValue> refFromRowValues = getConflictedRowContent(refFromEntity, diffRowValues, refToPrimaries, refFromAttributes);

        return refFromAttributes.stream()
                .flatMap(refFromAttribute ->
                        diffRowValues.stream()
                                .flatMap(diffRowValue -> {
                                    List<RefBookRowValue> rowValues =
                                            findRefBookRowValues(refToPrimaries, refFromAttribute, diffRowValue, refFromRowValues);
                                    return rowValues.stream()
                                            .map(rowValue ->
                                                    createDataDiffConflict(diffRowValue.getStatus(), rowValue, refFromAttribute, refFromEntity.getStructure()));
                                })
                ).collect(toList());
    }

    /**
     * Создание записи о diff-конфликте.
     *
     * @param diffStatus       статус diff-записи
     * @param refFromRowValue  запись из версии, которая ссылается
     * @param refFromAttribute ссылочный атрибут версии, которая ссылается
     * @param refFromStructure структура версии, которая ссылается
     * @return Запись о diff-конфликте
     */
    private Conflict createDataDiffConflict(DiffStatusEnum diffStatus, RefBookRowValue refFromRowValue,
                                            Structure.Attribute refFromAttribute, Structure refFromStructure) {
        Conflict conflict = new Conflict();
        conflict.setRefAttributeCode(refFromAttribute.getCode());
        conflict.setConflictType(diffStatusToConflictType(diffStatus));
        conflict.setPrimaryValues(getRowPrimaryValues(refFromRowValue, refFromStructure));

        return conflict;
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
        versionValidation.validateVersionExists(oldRefToId);
        versionValidation.validateVersionExists(newRefToId);

        if (ConflictType.ALTERED.equals(conflictType)) {
            // NB: Проверить сначала, есть ли реальные ссылки из refFromId.
            StructureDiff diff = compareService.compareStructures(oldRefToId, newRefToId);
            return isRefBookAltered(diff);
        }

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity oldRefToEntity = versionRepository.getOne(oldRefToId);

        DiffStatusEnum diffStatus = conflictTypeToDiffStatus(conflictType);

        CompareDataCriteria criteria = new CompareDataCriteria(oldRefToId, newRefToId);
        criteria.setOrders(SORT_VERSION_DATA);
        criteria.startPageNumber(FIRST_PAGE_NUMBER, REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

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
                                .anyMatch(diffRowValue -> {
                                    RefBookRowValue rowValue =
                                            findRefBookRowValue(refToPrimaries, refFromAttribute, diffRowValue, refFromRowValues);
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
        String refBookCode = versionEntity.getRefBook().getCode();

        List<RefBookVersion> conflictedReferrers = new ArrayList<>(REF_BOOK_VERSION_PAGE_SIZE);
        Consumer<List<RefBookVersion>> consumer = referrers -> {
            List<RefBookVersion> list = referrers.stream()
                    .filter(referrer -> {
                        Integer lastPublishedId = versionService.getLastPublishedVersion(refBookCode).getId();
                        return checkConflicts(referrer.getId(), lastPublishedId, versionId, conflictType);
                    })
                    .collect(toList());
            conflictedReferrers.addAll(list);
        };
        processReferrerVersions(refBookCode, RefBookSourceType.LAST_VERSION, consumer);

        return conflictedReferrers;
    }

    /**
     * Поиск конфликтов по критерию поиска.
     *
     * @param criteria критерий поиска
     * @return Страница конфликтов
     */
    @Override
    public Page<RefBookConflict> search(RefBookConflictCriteria criteria) {

        Page<RefBookConflictEntity> entities = findConflictEntities(criteria);
        return entities.map(this::refBookConflictModel);
    }

    private Page<RefBookConflictEntity> findConflictEntities(RefBookConflictCriteria criteria) {
        JPAQuery<RefBookConflictEntity> jpaQuery =
                new JPAQuery<>(entityManager)
                        .select(QRefBookConflictEntity.refBookConflictEntity)
                        .from(QRefBookConflictEntity.refBookConflictEntity)
                        .where(RefBookConflictPredicateProducer.toPredicate(criteria));

        long count = jpaQuery.fetchCount();

        sortQuery(jpaQuery, criteria);
        List<RefBookConflictEntity> entities = jpaQuery
                .offset(criteria.getOffset())
                .limit(criteria.getPageSize())
                .fetch();

        return new PageImpl<>(entities, criteria, count);
    }

    /**
     * Сохранение информации о конфликтах.
     */
    @Override
    @Transactional
    public void create(CreateConflictsRequest request) {
        if (Objects.isNull(request)
                || isEmpty(request.getConflicts()))
            return;

        List<RefBookConflictEntity> entities = request.getConflicts().stream()
                .map(conflict -> createRefBookConflictEntity(request.getRefFromId(), request.getRefToId(), conflict))
                .collect(toList());

        conflictRepository.saveAll(entities);
    }

    /**
     * Создание сущности из конфликта для сохранения.
     *
     * @param refFromId идентификатор версии справочника со ссылками
     * @param refToId   идентификатор версии изменённого справочника
     * @param conflict  конфликт
     * @return Сущность
     */
    private RefBookConflictEntity createRefBookConflictEntity(Integer refFromId, Integer refToId, Conflict conflict) {

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity refToEntity = versionRepository.getOne(refToId);

        return createRefBookConflictEntity(refFromEntity, refToEntity, conflict);
    }

    /**
     * Создание сущности из конфликта для сохранения.
     *
     * @param referrerEntity  версия справочника со ссылками
     * @param publishedEntity версия изменённого справочника
     * @param conflict        конфликт
     * @return Сущность
     */
    private RefBookConflictEntity createRefBookConflictEntity(RefBookVersionEntity referrerEntity, RefBookVersionEntity publishedEntity, Conflict conflict) {

        RefBookRowValue refFromRowValue = getRefFromRowValue(referrerEntity, conflict.getPrimaryValues());
        if (refFromRowValue == null)
            throw new NotFoundException(REFERRER_ROW_NOT_FOUND_EXCEPTION_CODE);

        return new RefBookConflictEntity(referrerEntity, publishedEntity,
                refFromRowValue.getSystemId(), conflict.getRefAttributeCode(), conflict.getConflictType());
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        conflictRepository.deleteById(id);
    }

    /**
     * Удаление конфликтов по заданному критерию.
     *
     * @param criteria критерий удаления
     */
    @Override
    @Transactional
    public void delete(DeleteRefBookConflictCriteria criteria) {

        JPADeleteClause jpaDelete =
                new JPADeleteClause(entityManager, QRefBookConflictEntity.refBookConflictEntity)
                        .where(QRefBookConflictEntity.refBookConflictEntity.id.in(
                                JPAExpressions.select(QRefBookConflictEntity.refBookConflictEntity.id)
                                        .from(QRefBookConflictEntity.refBookConflictEntity)
                                        .where(DeleteRefBookConflictPredicateProducer.toPredicate(criteria))
                        ));
        jpaDelete.execute();
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

        List<RefBookVersionEntity> publishedVersions = conflictRepository.findReferrerConflictedPublishedVersions(referrerVersionId, refRecordIds);
        if (isEmpty(publishedVersions))
            return emptyList();

        BooleanBuilder where = new BooleanBuilder();
        where.and(isSourceType(RefBookSourceType.ACTUAL));

        List<Integer> refBookIds = publishedVersions.stream().map(entity -> entity.getRefBook().getId()).distinct().collect(toList());
        where.and(isVersionOfRefBooks(refBookIds));

        Pageable pageRequest = PageRequest.of(0, refBookIds.size());
        List<RefBookVersionEntity> actualVersions = (where.getValue() != null) ? versionRepository.findAll(where.getValue(), pageRequest).getContent() : emptyList();
        List<Integer> actualIds = actualVersions.stream().map(RefBookVersionEntity::getId).collect(toList());

        return conflictRepository.findReferrerConflictedIds(referrerVersionId, actualIds, refRecordIds);
    }

    /**
     * Перевычисление существующих конфликтов справочников.
     *
     * @param refFromEntity  версия справочника, которая ссылается
     * @param oldRefToEntity версия справочника, на которую ссылались
     * @param newRefToId     идентификатор новой версии, на которую будут ссылаться
     * @param conflicts      страничный список конфликтов
     * @param isAltered      наличие изменения структуры
     * @return Список перевычисленных конфликтов для версии, которая ссылается
     */
    @SuppressWarnings("WeakerAccess") // for ConflictServiceTest
    public List<Conflict> recalculateConflicts(RefBookVersionEntity refFromEntity,
                                               RefBookVersionEntity oldRefToEntity, Integer newRefToId,
                                               List<RefBookConflict> conflicts, boolean isAltered) {

        List<Long> refFromSystemIds = conflicts.stream()
                .map(RefBookConflict::getRefRecordId)
                .collect(toList());

        List<RefBookRowValue> refFromRowValues = getSystemRowValues(refFromEntity.getId(), refFromSystemIds);
        List<ReferenceFilterValue> filterValues = toFilterValues(refFromEntity, oldRefToEntity, conflicts, refFromRowValues);
        List<DiffRowValue> diffRowValues = getRefToDiffRowValues(oldRefToEntity.getId(), newRefToId, filterValues);

        return recalculateConflicts(refFromEntity, oldRefToEntity, conflicts, refFromRowValues, diffRowValues, isAltered);
    }

    /**
     * Перевычисление существующих конфликтов справочников.
     *
     * @param refFromEntity    версия справочника, которая ссылается
     * @param refToEntity      версия справочника, на которую ссылались
     * @param conflicts        список конфликтов
     * @param refFromRowValues список записей версии справочника, которая ссылается
     * @param diffRowValues    список различий версий справочника, на которую ссылаются
     * @param isAltered        наличие изменения структуры
     * @return Список конфликтов
     */
    private List<Conflict> recalculateConflicts(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                                List<RefBookConflict> conflicts, List<RefBookRowValue> refFromRowValues,
                                                List<DiffRowValue> diffRowValues, boolean isAltered) {
        return conflicts.stream()
                .map(conflict -> {
                    RefBookRowValue refFromRowValue = refFromRowValues.stream()
                            .filter(rowValue -> rowValue.getSystemId().equals(conflict.getRefRecordId()))
                            .findFirst().orElse(null);
                    if (refFromRowValue == null)
                        return null;

                    return recalculateConflict(refFromEntity, refToEntity, conflict, refFromRowValue, diffRowValues, isAltered);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Перевычисление существующих конфликтов справочников.
     *
     * @param refFromEntity   версия справочника, которая ссылается
     * @param refToEntity     версия справочника, на которую ссылались
     * @param conflict        конфликт
     * @param refFromRowValue запись версии справочника, которая ссылается
     * @param diffRowValues   список различий версий справочника, на которую ссылаются
     * @param isAltered       наличие изменения структуры
     * @return Список конфликтов
     */
    private Conflict recalculateConflict(RefBookVersionEntity refFromEntity, RefBookVersionEntity refToEntity,
                                         RefBookConflict conflict, RefBookRowValue refFromRowValue,
                                         List<DiffRowValue> diffRowValues, boolean isAltered) {

        ReferenceFieldValue fieldValue = (ReferenceFieldValue) (refFromRowValue.getFieldValue(conflict.getRefFieldCode()));
        Structure.Reference refFromReference = refFromEntity.getStructure().getReference(conflict.getRefFieldCode());
        Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(refToEntity.getStructure());
        ReferenceFilterValue filterValue = new ReferenceFilterValue(refToAttribute, fieldValue);

        // NB: Extract to separated method `recalculateConflict`.
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
                if (isAltered)
                    return null; // Есть изменение структуры

                diffRowValue = findDiffRowValue(filterValue, diffRowValues);
                if (Objects.nonNull(diffRowValue))
                    return null; // Есть новые изменения

                break; // Есть только старое обновление

            default:
                break; // Нет старых конфликтов, только новые.
        }

        return toConflict(conflict, refFromEntity.getStructure(), refFromRowValue);
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
            refreshReferenceByPrimary(referrerEntity, reference, ConflictType.UPDATED);
            refreshReferenceByPrimary(referrerEntity, reference, ConflictType.ALTERED);
        });
    }

    /**
     * Обновление заданной ссылки в справочнике по таблице конфликтов.
     *
     * @param referrerEntity  версия справочника, который ссылается
     * @param reference       поле справочника, которое ссылается
     * @param conflictType    тип конфликта
     */
    private void refreshReferenceByPrimary(RefBookVersionEntity referrerEntity, Structure.Reference reference, ConflictType conflictType) {

        List<RefBookVersionEntity> publishedEntities =
                conflictRepository.findPublishedVersionsRefreshingByPrimary(
                        referrerEntity.getId(), reference.getAttribute(), conflictType);

        publishedEntities.forEach(publishedEntity ->
                refreshReferenceByPrimary(referrerEntity, reference, publishedEntity, conflictType)
        );
    }

    /**
     * Обновление заданной ссылки в справочнике,
     * связанном с указанным справочником, по таблице конфликтов.
     *
     * @param referrerEntity  версия справочника, который ссылается
     * @param reference       поле справочника, которое ссылается
     * @param publishedEntity версия справочника, на который ссылаются
     * @param conflictType    тип конфликта
     */
    private void refreshReferenceByPrimary(RefBookVersionEntity referrerEntity, Structure.Reference reference, RefBookVersionEntity publishedEntity, ConflictType conflictType) {

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(publishedEntity.getStructure());

        Reference updatedReference = new Reference(
                publishedEntity.getStorageCode(),
                publishedEntity.getFromDate(), // SYS_PUBLISH_TIME is not exist for draft
                referenceAttribute.getCode(),
                new DisplayExpression(reference.getDisplayExpression()),
                null, // Old value is not changed
                null // Display value will be recalculated
        );
        ReferenceFieldValue fieldValue = new ReferenceFieldValue(reference.getAttribute(), updatedReference);

        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(referrerEntity.getId());
        criteria.setPublishedVersionId(publishedEntity.getId());
        criteria.setRefFieldCode(reference.getAttribute());
        criteria.setConflictType(conflictType);
        criteria.setOrders(SORT_REF_BOOK_CONFLICTS);
        criteria.startPageNumber(FIRST_PAGE_NUMBER, REF_BOOK_CONFLICT_PAGE_SIZE);

        Function<RefBookConflictCriteria, Page<RefBookConflictEntity>> pageSource = this::findConflictEntities;
        PageIterator<RefBookConflictEntity, RefBookConflictCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<Object> systemIds = page.getContent().stream()
                    .map(RefBookConflictEntity::getRefRecordId)
                    .collect(toList());

            draftDataService.updateReferenceInRows(referrerEntity.getStorageCode(), fieldValue, systemIds);

            conflictRepository.deleteInBatch(page.getContent());
        });
    }

    /**
     * Обновление ссылок в связанных справочниках по таблице конфликтов.
     *
     * @param refBookCode код справочника, на который ссылаются
     */
    @Override
    @Transactional
    public void refreshLastReferrersByPrimary(String refBookCode) {

        Consumer<List<RefBookVersion>> consumer =
                referrers -> referrers.forEach(referrer -> refreshReferrerByPrimary(referrer.getId()));
        processReferrerVersions(refBookCode, RefBookSourceType.LAST_VERSION, consumer);
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

        StructureDiff structureDiff = compareService.compareStructures(oldVersionId, newVersionId);
        boolean isAltered = isRefBookAltered(structureDiff);

        Consumer<List<RefBookVersion>> consumer = referrers -> {
            if (isAltered) {
                createCalculatedStructureConflicts(referrers, oldVersionId, newVersionId, structureDiff);
            }
            createCalculatedDataConflicts(referrers, oldVersionId, newVersionId, isAltered);
            referrers.forEach(referrer -> createRecalculatedConflicts(referrer.getId(), oldVersionId, newVersionId, isAltered));
        };
        RefBookVersionEntity oldVersionEntity = versionRepository.getOne(oldVersionId);
        processReferrerVersions(oldVersionEntity.getRefBook().getCode(), RefBookSourceType.ALL, consumer);
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
     * Добавление сортировки в запрос на основе критерия.
     *
     * @param jpaQuery запрос
     * @param criteria критерий поиска
     */
    private void sortQuery(JPAQuery<RefBookConflictEntity> jpaQuery, RefBookConflictCriteria criteria) {

        List<Sort.Order> orders = criteria.getOrders();

        if (!isEmpty(orders)) {
            criteria.getOrders().stream()
                    .filter(Objects::nonNull)
                    .forEach(order -> addSortOrder(jpaQuery, order));
        }
    }

    /**
     * Добавление сортировки в запрос по заданному порядку.
     *
     * @param jpaQuery запрос поиска
     * @param order    порядок сортировки
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

        return new RefBookConflict(entity.getReferrerVersion().getId(), entity.getPublishedVersion().getId(),
                entity.getRefRecordId(), entity.getRefFieldCode(), entity.getConflictType(), entity.getCreationDate());
    }

    private Conflict toConflict(RefBookConflict refBookConflict, Structure referrerStructure, RefBookRowValue referrerRowValue) {
        if (refBookConflict == null)
            return null;

        Conflict conflict = new Conflict();
        conflict.setRefAttributeCode(refBookConflict.getRefFieldCode());
        conflict.setConflictType(refBookConflict.getConflictType());

        List<FieldValue> primaryValues = getRowPrimaryValues(referrerRowValue, referrerStructure);
        conflict.setPrimaryValues(primaryValues);

        return conflict;
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
        return getConflictedRowContent(refFromEntity.getId(), refFromEntity.getStorageCode(), refFromEntity.getStructure(),
                refFromEntity.getFromDate(), refFromEntity.getToDate(), filters);
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

        return rowValue.getFieldValues().stream()
                .filter(fieldValue ->
                        structure.getAttribute(fieldValue.getField()).getIsPrimary())
                .collect(toList());
    }

    /**
     * Получение записей по системным идентификаторам.
     *
     * @param versionId идентификатор версии
     * @param systemIds системные идентификаторы записей
     */
    private List<RefBookRowValue> getSystemRowValues(Integer versionId, List<Long> systemIds) {
        if (versionId == null)
            return emptyList();

        SearchDataCriteria criteria = new SearchDataCriteria();
        Set<List<AttributeFilter>> filterSet = systemIds.stream()
                .map(systemId -> new AttributeFilter(DataConstants.SYS_PRIMARY_COLUMN, BigInteger.valueOf(systemId), FieldType.INTEGER))
                .map(Collections::singletonList)
                .collect(toSet());
        criteria.setAttributeFilter(filterSet);

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

        return compareService.compareData(criteria).getRows().getContent();
    }

    /**
     * Получение diff-записей данных версий справочников для конфликтов.
     *
     * @param diffRowValues список всех различий
     * @param isAltered наличие изменения структуры
     * @return Список различий
     */
    private List<DiffRowValue> getDataDiffContent(Page<DiffRowValue> diffRowValues, boolean isAltered) {
        return diffRowValues.getContent().stream()
                .filter(diffRowValue -> isAltered
                        ? DiffStatusEnum.DELETED.equals(diffRowValue.getStatus())
                        : CALCULATING_DIFF_STATUS_LIST.contains(diffRowValue.getStatus()))
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
                                                      List<RefBookConflict> conflicts, List<RefBookRowValue> refFromRowValues) {
        return conflicts.stream()
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
     * Получение конфликтной записи по конфликтному полю записи.
     */
    private RefBookRowValue getRefFromRowValue(RefBookVersionEntity versionEntity, List<FieldValue> fieldValues) {
        if (versionEntity == null)
            return null;

        SearchDataCriteria criteria = new SearchDataCriteria();
        List<AttributeFilter> filterList = fieldValues.stream()
                .map(fieldValue -> {
                    FieldType fieldType = versionEntity.getStructure().getAttribute(fieldValue.getField()).getType();
                    return new AttributeFilter(fieldValue.getField(), fieldValue.getValue(), fieldType, SearchTypeEnum.EXACT);
                })
                .collect(toList());
        criteria.setAttributeFilter(singleton(filterList));

        Page<RefBookRowValue> rowValues = versionService.search(versionEntity.getId(), criteria);
        return (rowValues != null && !isEmpty(rowValues.getContent())) ? rowValues.getContent().get(0) : null;
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
                    Object value = diffFieldValue.getValue(diff.getStatus());

                    return refFromAttributes.stream()
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

    /**
     * Сохранение информации о конфликтах структуры.
     *
     * @param referrers     версии, которые ссылаются
     * @param oldRefToId    идентификатор старой версии, на которую ссылались
     * @param newRefToId    идентификатор новой версии, на которую будут ссылаться
     * @param structureDiff различие в структурах версий
     */
    private void createCalculatedStructureConflicts(List<RefBookVersion> referrers, Integer oldRefToId, Integer newRefToId, StructureDiff structureDiff) {

        RefBookVersionEntity oldRefToEntity = versionRepository.getOne(oldRefToId);
        RefBookVersionEntity newRefToEntity = versionRepository.getOne(newRefToId);

        referrers.forEach(referrer -> {
            RefBookVersionEntity refFromEntity = versionRepository.getOne(referrer.getId());
            List<Structure.Reference> refFromReferences = refFromEntity.getStructure().getRefCodeReferences(oldRefToEntity.getRefBook().getCode());

            createCalculatedStructureAbsentConflicts(refFromEntity, newRefToEntity, refFromReferences, structureDiff);
            createCalculatedStructureAlterConflicts(refFromEntity, oldRefToEntity, newRefToEntity, refFromReferences);
        });
    }

    /**
     * Создание конфликтов, связанных с отсутствием кода атрибута в displayExpression.
     *
     * @param refFromEntity     версия, которая ссылается
     * @param newRefToEntity    новая версия, на которую будут ссылаться
     * @param refFromReferences ссылки версии, которая ссылается
     * @param structureDiff     различие в структурах версий
     */
    private void createCalculatedStructureAbsentConflicts(RefBookVersionEntity refFromEntity,
                                                          RefBookVersionEntity newRefToEntity,
                                                          List<Structure.Reference> refFromReferences,
                                                          StructureDiff structureDiff) {
        List<String> deletedCodes = structureDiff.getDeleted().stream()
                .map(attributeDiff -> attributeDiff.getOldAttribute().getCode())
                .collect(toList());
        if (StringUtils.isEmpty(deletedCodes))
            return;

        List<RefBookConflictEntity> conflictEntities = refFromReferences.stream()
                .filter(reference -> containsAnyPlaceholder(reference.getDisplayExpression(), deletedCodes))
                .map(reference ->
                        new RefBookConflictEntity(refFromEntity, newRefToEntity,
                                null, reference.getAttribute(), ConflictType.ABSENT))
                .collect(toList());

        if (!isEmpty(conflictEntities))
            conflictRepository.saveAll(conflictEntities);
    }

    /**
     * Создание конфликтов, связанных с изменением структуры.
     *
     * @param refFromEntity     версия, которая ссылается
     * @param oldRefToEntity    старая версия, на которую ссылаются
     * @param newRefToEntity    новая версия, на которую будут ссылаться
     * @param refFromReferences ссылки версии, которая ссылается
     */
    private void createCalculatedStructureAlterConflicts(RefBookVersionEntity refFromEntity,
                                                         RefBookVersionEntity oldRefToEntity,
                                                         RefBookVersionEntity newRefToEntity,
                                                         List<Structure.Reference> refFromReferences) {
        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setOrders(SORT_VERSION_DATA);
        criteria.startPageNumber(FIRST_PAGE_NUMBER, REF_BOOK_VERSION_DATA_PAGE_SIZE);

        Function<SearchDataCriteria, Page<RefBookRowValue>> pageSource = pageCriteria -> versionService.search(refFromEntity.getId(), criteria);
        PageIterator<RefBookRowValue, SearchDataCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<RefBookRowValue> refFromRows = page.getContent();

            refFromReferences.forEach(refFromReference -> {
                // NB: extract to `createCalculatedStructureConflicts`.
                Structure.Attribute refToAttribute = refFromReference.findReferenceAttribute(oldRefToEntity.getStructure());

                List<AbstractMap.SimpleEntry<Long, ReferenceFieldValue>> fieldEntries = refFromRows.stream()
                        .map(refFromRow -> {
                            ReferenceFieldValue referenceFieldValue = (ReferenceFieldValue) (refFromRow.getFieldValue(refFromReference.getAttribute()));
                            if (!StringUtils.isEmpty(referenceFieldValue.getValue()))
                                return new AbstractMap.SimpleEntry<>(refFromRow.getSystemId(), referenceFieldValue);

                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(toList());

                List<ReferenceFilterValue> filterValues = fieldEntries.stream()
                        .map(fieldEntry -> new ReferenceFilterValue(refToAttribute, fieldEntry.getValue()))
                        .collect(toList());
                List<RefBookRowValue> refToRowValues = getRefToRowValues(newRefToEntity.getId(), filterValues);

                List<RefBookConflictEntity> conflictEntities = fieldEntries.stream()
                        .map(fieldEntry -> {
                            ReferenceFieldValue referenceFieldValue = fieldEntry.getValue();
                            boolean hasFieldRow = refToRowValues.stream()
                                    .anyMatch(refToRow -> {
                                        FieldValue fieldValue = refToRow.getFieldValue(refToAttribute.getCode());
                                        return Objects.equals(fieldValue.getValue(),
                                                castFieldValue(referenceFieldValue, refToAttribute.getType()));
                                    });
                            if (hasFieldRow) {
                                return new RefBookConflictEntity(refFromEntity, newRefToEntity,
                                        fieldEntry.getKey(), referenceFieldValue.getField(), ConflictType.ALTERED);
                            }

                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(toList());

                conflictRepository.saveAll(conflictEntities);
            });
        });
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

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setAttributeFilter(toAttributeFilters(filterValues));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return (rowValues != null && !isEmpty(rowValues.getContent())) ? rowValues.getContent() : emptyList();
    }

    /**
     * Сохранение информации о вычисленных конфликтах версий.
     *
     * @param referrers  версии, которые ссылаются
     * @param oldRefToId идентификатор старой версии, на которую ссылались
     * @param newRefToId идентификатор новой версии, на которую будут ссылаться
     * @param isAltered  наличие изменения структуры
     */
    private void createCalculatedDataConflicts(List<RefBookVersion> referrers, Integer oldRefToId, Integer newRefToId, boolean isAltered) {

        referrers.forEach(referrer -> createCalculatedDataConflicts(referrer.getId(), oldRefToId, newRefToId, isAltered));
    }

    /**
     * Сохранение информации о вычисленных конфликтах версии.
     *
     * @param referrerId идентификатор версии, которая ссылается
     * @param oldRefToId идентификатор старой версии, на которую ссылались
     * @param newRefToId идентификатор новой версии, на которую будут ссылаться
     * @param isAltered  наличие изменения структуры
     */
    private void createCalculatedDataConflicts(Integer referrerId, Integer oldRefToId, Integer newRefToId, boolean isAltered) {

        CompareDataCriteria criteria = new CompareDataCriteria(oldRefToId, newRefToId);
        criteria.setOrders(SORT_VERSION_DATA);
        criteria.setPageNumber(FIRST_PAGE_NUMBER);
        criteria.setPageSize(REF_BOOK_DIFF_CONFLICT_PAGE_SIZE);

        RefBookVersionEntity referrerVersionEntity = versionRepository.getOne(referrerId);
        RefBookVersionEntity oldVersionEntity = versionRepository.getOne(oldRefToId);

        Function<CompareDataCriteria, Page<DiffRowValue>> pageSource = pageCriteria -> compareService.compareData(pageCriteria).getRows();
        PageIterator<DiffRowValue, CompareDataCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<Conflict> conflicts = calculateDataDiffConflicts(referrerVersionEntity, oldVersionEntity, getDataDiffContent(page, isAltered));
            create(new CreateConflictsRequest(referrerId, newRefToId, conflicts));
        });
    }

    /**
     * Сохранение информации о перевычисленных конфликтах.
     *
     * @param refFromId  идентификатор версии, которая ссылается
     * @param oldRefToId идентификатор старой версии, на которую ссылались
     * @param newRefToId идентификатор новой версии, на которую будут ссылаться
     * @param isAltered  наличие изменения структуры
     */
    private void createRecalculatedConflicts(Integer refFromId, Integer oldRefToId, Integer newRefToId, boolean isAltered) {

        RefBookVersionEntity refFromEntity = versionRepository.getOne(refFromId);
        RefBookVersionEntity oldRefToEntity = versionRepository.getOne(oldRefToId);

        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(refFromId);
        criteria.setPublishedVersionId(oldRefToId);
        criteria.setOrders(SORT_REF_BOOK_CONFLICTS);
        criteria.startPageNumber(FIRST_PAGE_NUMBER, REF_BOOK_CONFLICT_PAGE_SIZE);

        Function<RefBookConflictCriteria, Page<RefBookConflict>> pageSource = this::search;
        PageIterator<RefBookConflict, RefBookConflictCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> {
            List<Conflict> conflicts = recalculateConflicts(refFromId, oldRefToId, newRefToId, page.getContent(), isAltered);
            create(new CreateConflictsRequest(refFromId, newRefToId, conflicts));
        });
    }

    /**
     * Обработка версий справочников, ссылающихся на указанный справочник.
     *
     * @param refBookCode код справочника, на который ссылаются
     * @param sourceType  тип выбираемых версий справочников
     * @param consumer    обработчик списков версий
     */
    private void processReferrerVersions(String refBookCode, RefBookSourceType sourceType, Consumer<List<RefBookVersion>> consumer) {

        ReferrerVersionCriteria criteria = new ReferrerVersionCriteria(refBookCode, RefBookStatusType.USED, sourceType);
        criteria.setOrders(SORT_REFERRER_VERSIONS);
        criteria.startPageNumber(FIRST_PAGE_NUMBER, REF_BOOK_VERSION_PAGE_SIZE);

        Function<ReferrerVersionCriteria, Page<RefBookVersion>> pageSource = refBookService::searchReferrerVersions;
        PageIterator<RefBookVersion, ReferrerVersionCriteria> pageIterator = new PageIterator<>(pageSource, criteria);
        pageIterator.forEachRemaining(page -> consumer.accept(page.getContent()));
    }
}
