package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.PassportValueRepository;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.util.ModelGenerator;
import ru.inovus.ms.rdm.util.PassportPredicateProducer;
import ru.inovus.ms.rdm.validation.VersionValidation;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

@Primary
@Service
public class RefBookServiceImpl implements RefBookService {

    private static final String PASSPORT_SORT_PREFIX = "passport";
    private static final String VERSION_ID_SORT_PROPERTY = "id";
    private static final String REF_BOOK_ID_SORT_PROPERTY = "refbookId";
    private static final String REF_BOOK_CODE_SORT_PROPERTY = "code";
    private static final String REF_BOOK_DISPLAY_CODE_SORT_PROPERTY = "displayCode";
    private static final String REF_BOOK_LAST_PUBLISH_DATE_SORT_PROPERTY = "lastPublishedVersionFromDate";
    private static final String REF_BOOK_FROM_DATE_SORT_PROPERTY = "fromDate";
    private static final String REF_BOOK_CATEGORY_SORT_PROPERTY = "category";

    private static final String REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE = "refbook.already.exists";
    private static final String CANNOT_ORDER_BY_EXCEPTION_CODE = "cannot.order.by \"{0}\"";

    private RefBookRepository refBookRepository;
    private RefBookVersionRepository versionRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;

    private RefBookLockService refBookLockService;

    private VersionValidation versionValidation;

    private PassportValueRepository passportValueRepository;
    private PassportPredicateProducer passportPredicateProducer;

    private EntityManager entityManager;

    @Autowired
    @SuppressWarnings("all")
    public RefBookServiceImpl(RefBookRepository refBookRepository, RefBookVersionRepository versionRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              RefBookLockService refBookLockService, VersionValidation versionValidation,
                              PassportValueRepository passportValueRepository, PassportPredicateProducer passportPredicateProducer,
                              EntityManager entityManager) {
        this.refBookRepository = refBookRepository;
        this.versionRepository = versionRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;

        this.versionValidation = versionValidation;

        this.passportValueRepository = passportValueRepository;
        this.passportPredicateProducer = passportPredicateProducer;

        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Page<RefBook> search(RefBookCriteria criteria) {
        JPAQuery<RefBookVersionEntity> jpaQuery =
                new JPAQuery<>(entityManager)
                        .select(QRefBookVersionEntity.refBookVersionEntity)
                        .from(QRefBookVersionEntity.refBookVersionEntity)
                        .where(toPredicate(criteria));

        long count = jpaQuery.fetchCount();

        sortQuery(jpaQuery, criteria);
        List<RefBookVersionEntity> refBookVersionEntityList = jpaQuery
                .offset(criteria.getOffset())
                .limit(criteria.getPageSize())
                .fetch();

        Page<RefBookVersionEntity> list = new PageImpl<>(refBookVersionEntityList, criteria, count);
        List<Integer> refBookIdsInPage = list.getContent()
                .stream()
                .map(v -> v.getRefBook().getId())
                .collect(toList());
        return list.map(entity ->
                refBookModel(entity,
                        getSourceTypeVersions(refBookIdsInPage, RefBookSourceType.DRAFT),
                        getSourceTypeVersions(refBookIdsInPage, RefBookSourceType.LAST_PUBLISHED))
        );
    }

    private void sortQuery(JPAQuery<RefBookVersionEntity> jpaQuery, RefBookCriteria criteria) {
        List<Sort.Order> orders = criteria.getOrders();

        if (CollectionUtils.isEmpty(orders)) {
            jpaQuery.orderBy(getOrderByLastPublishDateExpression(jpaQuery).asc());
        } else {
            criteria.getOrders().stream()
                    .filter(order -> order != null && order.getProperty() != null)
                    .forEach(order -> addOrder(jpaQuery, order));
        }
    }

    private void addOrder(JPAQuery<RefBookVersionEntity> jpaQuery, Sort.Order order) {
        ComparableExpressionBase sortExpression;

        if (order.getProperty().startsWith(PASSPORT_SORT_PREFIX)) {
            String property = order.getProperty().replaceFirst(PASSPORT_SORT_PREFIX + "\\.", "");
            QPassportValueEntity qPassportValueEntity = new QPassportValueEntity(PASSPORT_SORT_PREFIX + "_" + property);

            jpaQuery.leftJoin(QRefBookVersionEntity.refBookVersionEntity.passportValues, qPassportValueEntity)
                    .on(qPassportValueEntity.version.eq(QRefBookVersionEntity.refBookVersionEntity)
                            .and(qPassportValueEntity.attribute.code.eq(property)));
            sortExpression = qPassportValueEntity.value;

        } else {
            switch (order.getProperty()) {
                case VERSION_ID_SORT_PROPERTY:
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.id;
                    break;

                case REF_BOOK_ID_SORT_PROPERTY:
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.refBook.id;
                    break;

                case REF_BOOK_CODE_SORT_PROPERTY:
                case REF_BOOK_DISPLAY_CODE_SORT_PROPERTY:
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.refBook.code;
                    break;

                case REF_BOOK_LAST_PUBLISH_DATE_SORT_PROPERTY:
                    sortExpression = getOrderByLastPublishDateExpression(jpaQuery);
                    break;

                case REF_BOOK_FROM_DATE_SORT_PROPERTY:
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.fromDate;
                    break;

                case REF_BOOK_CATEGORY_SORT_PROPERTY:
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.refBook.category;
                    break;

                default:
                    throw new UserException(new Message(CANNOT_ORDER_BY_EXCEPTION_CODE, order.getProperty()));
            }
        }
        jpaQuery.orderBy(order.isAscending() ? sortExpression.asc() : sortExpression.desc());
    }

    private ComparableExpressionBase getOrderByLastPublishDateExpression(JPAQuery<RefBookVersionEntity> jpaQuery) {
        QRefBookVersionEntity qSortFromDateVersion = new QRefBookVersionEntity("sort_from_date");
        QRefBookVersionEntity whereVersion = new QRefBookVersionEntity("sort_max_version");

        jpaQuery.leftJoin(qSortFromDateVersion)
                .on(QRefBookVersionEntity.refBookVersionEntity.refBook.eq(qSortFromDateVersion.refBook)
                        .and(qSortFromDateVersion.fromDate.eq(JPAExpressions
                                .select(whereVersion.fromDate.max()).from(whereVersion)
                                .where(whereVersion.refBook.eq(QRefBookVersionEntity.refBookVersionEntity.refBook)))));
        return qSortFromDateVersion.fromDate;
    }

    @Override
    @Transactional
    public RefBook getByVersionId(Integer versionId) {

        versionValidation.validateVersionExists(versionId);

        RefBookVersionEntity version = versionRepository.getOne(versionId);
        return refBookModel(version,
                getSourceTypeVersion(version.getRefBook().getId(), RefBookSourceType.DRAFT),
                getSourceTypeVersion(version.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED));
    }

    @Override
    @Transactional
    public String getCode(Integer refBookId) {

        versionValidation.validateRefBookExists(refBookId);

        final RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        return refBookEntity.getCode();
    }

    @Override
    @Transactional
    public Integer getId(String refBookCode) {

        final RefBookEntity refBookEntity = refBookRepository.findByCode(refBookCode);
        return refBookEntity.getId();
    }

    @Override
    @Transactional
    public RefBook create(RefBookCreateRequest request) {
        if (refBookRepository.existsByCode(request.getCode()))
            throw new UserException(new Message(REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE, request.getCode()));

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setArchived(Boolean.FALSE);
        refBookEntity.setRemovable(Boolean.TRUE);
        refBookEntity.setCode(request.getCode());
        refBookEntity.setCategory(request.getCategory());
        refBookEntity = refBookRepository.save(refBookEntity);

        RefBookVersionEntity refBookVersionEntity = new RefBookVersionEntity();
        populateVersionFromPassport(refBookVersionEntity, request.getPassport());
        refBookVersionEntity.setRefBook(refBookEntity);
        refBookVersionEntity.setStatus(RefBookVersionStatus.DRAFT);

        String storageCode = draftDataService.createDraft(Collections.emptyList());
        refBookVersionEntity.setStorageCode(storageCode);
        Structure structure = new Structure();
        structure.setAttributes(Collections.emptyList());
        structure.setReferences(Collections.emptyList());
        refBookVersionEntity.setStructure(structure);

        RefBookVersionEntity savedVersion = versionRepository.save(refBookVersionEntity);
        return refBookModel(savedVersion,
                getSourceTypeVersion(savedVersion.getRefBook().getId(), RefBookSourceType.DRAFT),
                getSourceTypeVersion(savedVersion.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED));
    }

    @Override
    @Transactional
    public RefBook update(RefBookUpdateRequest request) {

        versionValidation.validateVersion(request.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(request.getVersionId());

        RefBookVersionEntity versionEntity = versionRepository.getOne(request.getVersionId());
        RefBookEntity refBookEntity = versionEntity.getRefBook();
        if (!refBookEntity.getCode().equals(request.getCode())) {
            if (refBookRepository.existsByCode((request.getCode())))
                throw new UserException(new Message(REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE, request.getCode()));

            refBookEntity.setCode(request.getCode());
        }
        refBookEntity.setCategory(request.getCategory());
        updateVersionFromPassport(versionEntity, request.getPassport());
        versionEntity.setComment(request.getComment());
        return refBookModel(versionEntity,
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.DRAFT),
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED));
    }

    @Override
    @Transactional
    public void delete(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);

        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        refBookLockService.validateRefBookNotBusy(refBookEntity);

        refBookEntity.getVersionList().forEach(v ->
                dropDataService.drop(refBookRepository.getOne(refBookId).getVersionList().stream()
                        .map(RefBookVersionEntity::getStorageCode)
                        .collect(Collectors.toSet())));
        refBookRepository.deleteById(refBookId);
    }

    @Override
    @Transactional
    public void toArchive(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);

        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        // NB: Add checking references to this refBook.
        refBookEntity.setArchived(Boolean.TRUE);
        refBookRepository.save(refBookEntity);
    }

    @Override
    @Transactional
    public void fromArchive(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);

        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        refBookEntity.setArchived(Boolean.FALSE);
        refBookRepository.save(refBookEntity);
    }

    @Override
    @Transactional
    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {
        criteria.setOrders(singletonList(new Sort.Order(Sort.Direction.DESC, REF_BOOK_FROM_DATE_SORT_PROPERTY, Sort.NullHandling.NULLS_FIRST)));
        Page<RefBookVersionEntity> list = versionRepository.findAll(toPredicate(criteria), criteria);
        return list.map(ModelGenerator::versionModel);
    }

    private Predicate toPredicate(VersionCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(criteria.getRefBookId()));
        if (criteria.getExcludeDraft())
            where.andNot(isDraft());
        if (nonNull(criteria.getVersion()))
            where.and(isVersionNumberContains(criteria.getVersion()));
        return where.getValue();
    }

    private Predicate toPredicate(RefBookCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(isSourceType(criteria.getRefBookSourceType()));

        if (nonNull(criteria.getFromDateBegin()))
            where.and(isMaxFromDateEqOrAfter(criteria.getFromDateBegin()));

        if (nonNull(criteria.getFromDateEnd()))
            where.and(isMaxFromDateEqOrBefore(criteria.getFromDateEnd()));

        if (!isEmpty(criteria.getCode()))
            where.and(isCodeContains(criteria.getCode()));

        if (!CollectionUtils.isEmpty(criteria.getPassport()))
            where.and(passportPredicateProducer.toPredicate(criteria.getPassport()));

        if (!isEmpty(criteria.getCategory()))
            where.and(refBookHasCategory(criteria.getCategory()));

        if (!CollectionUtils.isEmpty(criteria.getRefBookIds()))
            where.and(isVersionOfRefBook(criteria.getRefBookIds()));

        if (criteria.getIsArchived())
            where.and(isArchived());

        if (criteria.getHasPublished())
            where.andNot(isArchived()).and(isAnyPublished());

        if (criteria.getHasDraft())
            where.andNot(isArchived()).and(refBookHasDraft());

        if (criteria.getHasPublishedVersion())
            where.andNot(isArchived()).and(hasLastPublishedVersion());

        if (criteria.getHasPrimaryAttribute())
            where.and(hasPrimaryAttribute());

        return where.getValue();
    }

    // NB: Необходим также для отображения справочников, ссылающихся на текущий справочник.

    /**
     * Поиск версий справочников, ссылающихся на указанный справочник.
     *
     * Ссылающийся справочник должен иметь:
     *   1) структуру,
     *   2) первичный ключ,
     *   3) ссылку на указанный справочник.
     *
     * @param refBookCode код справочника, на который ссылаются
     * @param sourceType  типа выбираемых версий справочников
     * @param referrerIds список идентификаторов ссылающихся справочников
     * @return Список справочников
     */
    @Override
    @Transactional
    public List<RefBookVersion> getReferrerVersions(String refBookCode, RefBookSourceType sourceType, List<Integer> referrerIds) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isSourceType(sourceType)).andNot(isArchived());

        if (!CollectionUtils.isEmpty(referrerIds))
            where.and(isVersionOfRefBook(referrerIds));

        Page<RefBookVersionEntity> allEntities = versionRepository.findAll(where, Pageable.unpaged());
        List<RefBookVersionEntity> entities = StreamSupport
                .stream(allEntities.spliterator(), false)
                .filter(entity ->
                        Objects.nonNull(entity.getStructure())
                                && !CollectionUtils.isEmpty(entity.getStructure().getPrimary())
                                && !entity.getStructure().getRefCodeReferences(refBookCode).isEmpty())
                .collect(Collectors.toList());

        return entities.stream().map(ModelGenerator::versionModel).collect(Collectors.toList());
    }

    private boolean isRefBookRemovable(Integer refBookId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(refBookId));
        where.and(isRemovable().not().or(isArchived()).or(isPublished()));
        return (where.getValue() != null) && !versionRepository.exists(where.getValue());
    }

    private RefBook refBookModel(RefBookVersionEntity entity, List<RefBookVersionEntity> draftVersions, List<RefBookVersionEntity> lastPublishVersions) {
        if (entity == null) return null;

        RefBookVersionEntity draftVersion = getRefBookSourceTypeVersion(entity.getRefBook().getId(), draftVersions);
        RefBookVersionEntity lastPublishedVersion = getRefBookSourceTypeVersion(entity.getRefBook().getId(), lastPublishVersions);
        return refBookModel(entity, draftVersion, lastPublishedVersion);
    }

    private RefBook refBookModel(RefBookVersionEntity entity, RefBookVersionEntity draftVersion, RefBookVersionEntity lastPublishedVersion) {
        if (entity == null) return null;

        RefBook model = new RefBook(ModelGenerator.versionModel(entity));
        model.setStatus(entity.getStatus());
        model.setRemovable(isRefBookRemovable(entity.getRefBook().getId()));
        model.setCategory(entity.getRefBook().getCategory());

        if (draftVersion != null) {
            model.setDraftVersionId(draftVersion.getId());
        }

        if (lastPublishedVersion != null) {
            model.setLastPublishedVersionId(lastPublishedVersion.getId());
            model.setLastPublishedVersion(lastPublishedVersion.getVersion());
            model.setLastPublishedVersionFromDate(lastPublishedVersion.getFromDate());
        }

        Structure structure = entity.getStructure();
        List<Structure.Attribute> primaryAttributes = (structure != null) ? structure.getPrimary() : null;
        model.setHasPrimaryAttribute(!CollectionUtils.isEmpty(primaryAttributes));

        return model;
    }

    private void populateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, String> passport) {
        if (passport != null && versionEntity != null) {
            versionEntity.setPassportValues(passport.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> new PassportValueEntity(new PassportAttributeEntity(e.getKey()), e.getValue(), versionEntity))
                    .collect(toList()));
        }
    }

    private void updateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, String> newPassport) {
        if (newPassport == null || versionEntity == null) {
            return;
        }

        List<PassportValueEntity> newPassportValues = versionEntity.getPassportValues() != null ?
                versionEntity.getPassportValues() : new ArrayList<>();

        Map<String, String> correctUpdatePassport = new HashMap<>(newPassport);

        Set<String> attributeCodesToRemove = correctUpdatePassport.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        List<PassportValueEntity> toRemove = versionEntity.getPassportValues().stream()
                .filter(v -> attributeCodesToRemove.contains(v.getAttribute().getCode()))
                .collect(toList());
        versionEntity.getPassportValues().removeAll(toRemove);
        passportValueRepository.deleteAll(toRemove);
        correctUpdatePassport.entrySet().removeIf(e -> attributeCodesToRemove.contains(e.getKey()));

        Set<Map.Entry> toUpdate = correctUpdatePassport.entrySet().stream()
                .filter(e -> newPassportValues.stream().anyMatch(v -> e.getKey().equals(v.getAttribute().getCode())))
                .peek(e -> newPassportValues.stream()
                        .filter(v -> e.getKey().equals(v.getAttribute().getCode()))
                        .findAny().get().setValue(e.getValue()))
                .collect(Collectors.toSet());
        correctUpdatePassport.entrySet().removeAll(toUpdate);

        newPassportValues.addAll(correctUpdatePassport.entrySet().stream()
                .map(a -> new PassportValueEntity(new PassportAttributeEntity(a.getKey()), a.getValue(), versionEntity))
                .collect(toList()));

        versionEntity.setPassportValues(newPassportValues);
    }

    /**
     * Получение списка версий справочников с заданным типом источника.
     *
     * @param refBookIds        список идентификаторов справочников
     * @param refBookSourceType источник данных справочника
     * @return Список требуемых версий справочников
     */
    private List<RefBookVersionEntity> getSourceTypeVersions(List<Integer> refBookIds, RefBookSourceType refBookSourceType) {
        RefBookCriteria versionCriteria = new RefBookCriteria();
        versionCriteria.setRefBookSourceType(refBookSourceType);
        versionCriteria.setRefBookIds(refBookIds);

        return versionRepository.findAll(toPredicate(versionCriteria),
                PageRequest.of(0, refBookIds.size())).getContent();
    }

    private RefBookVersionEntity getSourceTypeVersion(Integer refBookId, RefBookSourceType refBookSourceType) {

        List<RefBookVersionEntity> versions = getSourceTypeVersions(singletonList(refBookId), refBookSourceType);
        return getRefBookSourceTypeVersion(refBookId, versions);
    }

    private RefBookVersionEntity getRefBookSourceTypeVersion(Integer refBookId, List<RefBookVersionEntity> versions) {
        return versions.stream()
                .filter(v -> v.getRefBook().getId().equals(refBookId))
                .findAny().orElse(null);
    }
}
