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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.RefBookInfo;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.PassportValueRepository;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.util.ModelGenerator;
import ru.inovus.ms.rdm.util.PassportPredicateProducer;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

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
    private static final String REF_BOOK_LAST_PUBLISH_SORT_PROPERTY = "lastPublishedVersionFromDate";
    private static final String REF_BOOK_FROM_DATE_SORT_PROPERTY = "fromDate";
    private static final String REF_BOOK_CATEGORY_SORT_PROPERTY = "category";

    private static final String REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE = "refbook.already.exists";

    private RefBookVersionRepository repository;
    private RefBookRepository refBookRepository;
    private DraftDataService draftDataService;
    private DropDataService dropDataService;
    private PassportValueRepository passportValueRepository;
    private PassportPredicateProducer passportPredicateProducer;
    private EntityManager entityManager;
    private RefBookLockService refBookLockService;

    @Autowired
    @SuppressWarnings("all")
    public RefBookServiceImpl(RefBookVersionRepository repository, RefBookRepository refBookRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              PassportValueRepository passportValueRepository, PassportPredicateProducer passportPredicateProducer,
                              EntityManager entityManager, RefBookLockService refBookLockService) {
        this.repository = repository;
        this.refBookRepository = refBookRepository;
        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
        this.passportValueRepository = passportValueRepository;
        this.passportPredicateProducer = passportPredicateProducer;
        this.entityManager = entityManager;
        this.refBookLockService = refBookLockService;
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
        return list.map(entity -> refBookModel(entity, getLastPublishedVersions(refBookIdsInPage)));
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
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.refBook.code;
                    break;
                case REF_BOOK_LAST_PUBLISH_SORT_PROPERTY:
                    sortExpression = getOrderByLastPublishDateExpression(jpaQuery);
                    break;
                case REF_BOOK_FROM_DATE_SORT_PROPERTY:
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.fromDate;
                    break;
                case REF_BOOK_CATEGORY_SORT_PROPERTY:
                    sortExpression = QRefBookVersionEntity.refBookVersionEntity.refBook.category;
                    break;
                default:
                    throw new UserException(new Message("cannot.order.by", order.getProperty()));
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

        validateVersionExists(versionId);

        RefBookVersionEntity refBookVersion = repository.findOne(versionId);
        return refBookModel(refBookVersion, getLastPublishedVersions(singletonList(refBookVersion.getRefBook().getId())));
    }

    @Override
    @Transactional
    public String getCode(Integer refBookId) {
        validateRefBookExists(refBookId);
        final RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        return refBookEntity.getCode();
    }

    @Override
    @Transactional
    public RefBook create(RefBookCreateRequest request) {
        if (isRefBookExist(request.getCode()))
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

        RefBookVersionEntity savedVersion = repository.save(refBookVersionEntity);
        return refBookModel(savedVersion, getLastPublishedVersions(singletonList(savedVersion.getRefBook().getId())));
    }

    @Override
    @Transactional
    public RefBook update(RefBookUpdateRequest request) {
        validateVersionExists(request.getVersionId());
        validateVersionNotArchived(request.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(request.getVersionId());

        RefBookVersionEntity refBookVersionEntity = repository.findOne(request.getVersionId());
        RefBookEntity refBookEntity = refBookVersionEntity.getRefBook();
        if (!refBookEntity.getCode().equals(request.getCode())) {
            if (isRefBookExist(request.getCode()))
                throw new UserException(new Message(REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE, request.getCode()));

            refBookEntity.setCode(request.getCode());
        }
        refBookEntity.setCategory(request.getCategory());
        updateVersionFromPassport(refBookVersionEntity, request.getPassport());
        refBookVersionEntity.setComment(request.getComment());
        return refBookModel(refBookVersionEntity, getLastPublishedVersions(singletonList(refBookVersionEntity.getRefBook().getId())));
    }

    @Override
    @Transactional
    public void delete(int refBookId) {

        validateRefBookExists(refBookId);
        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        refBookLockService.validateRefBookNotBusy(refBookEntity);

        refBookEntity.getVersionList().forEach(v ->
                dropDataService.drop(refBookRepository.getOne(refBookId).getVersionList().stream()
                        .map(RefBookVersionEntity::getStorageCode)
                        .collect(Collectors.toSet())));
        refBookRepository.delete(refBookId);
    }

    @Override
    public void toArchive(int refBookId) {
        validateRefBookExists(refBookId);
        RefBookEntity refBookEntity = refBookRepository.findOne(refBookId);
        refBookEntity.setArchived(Boolean.TRUE);
        refBookRepository.save(refBookEntity);
    }

    @Override
    public void fromArchive(int refBookId) {
        validateRefBookExists(refBookId);
        RefBookEntity refBookEntity = refBookRepository.findOne(refBookId);
        refBookEntity.setArchived(Boolean.FALSE);
        refBookRepository.save(refBookEntity);
    }

    @Override
    @Transactional
    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {
        criteria.setOrders(singletonList(new Sort.Order(Sort.Direction.DESC, REF_BOOK_FROM_DATE_SORT_PROPERTY, Sort.NullHandling.NULLS_FIRST)));
        Page<RefBookVersionEntity> list = repository.findAll(toPredicate(criteria), criteria);
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

        if (RefBookInfo.ACTUAL.equals(criteria.getRefBookInfo()))
            where.and(isActual());
        else if (RefBookInfo.PUBLISHED.equals(criteria.getRefBookInfo()))
            where.and(isLastPublished());
        else
            where.and(refBookHasDraft().not().and(isLastPublished()).or(isDraft()));

        if (nonNull(criteria.getFromDateBegin()))
            where.and(isMaxFromDateEqOrAfter(criteria.getFromDateBegin()));

        if (nonNull(criteria.getFromDateEnd()))
            where.and(isMaxFromDateEqOrBefore(criteria.getFromDateEnd()));

        if (!isEmpty(criteria.getCode()))
            where.and(isCodeContains(criteria.getCode()));

        if (!CollectionUtils.isEmpty(criteria.getPassport())) {
            where.and(passportPredicateProducer.toPredicate(criteria.getPassport()));
        }

        if (!isEmpty(criteria.getCategory())) {
            where.and(refBookHasCategory(criteria.getCategory()));
        }

        if (!CollectionUtils.isEmpty(criteria.getRefBookIds())) {
            where.and(isVersionOfRefBook(criteria.getRefBookIds()));
        }

        if (criteria.getIsArchived()) {
            where.and(isArchived());
        }

        if (criteria.getHasPublished()) {
            where.andNot(isArchived()).and(isAnyPublished());
        }

        if (criteria.getHasDraft()) {
            where.andNot(isArchived()).and(refBookHasDraft());
        }

        return where.getValue();
    }

    private boolean isRefBookRemovable(Integer refBookId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(refBookId));
        where.and(isRemovable().not().or(isArchived()).or(isPublished()));
        return !repository.exists(where.getValue());
    }

    private boolean isRefBookExist(String refBookCode) {
        RefBookEntity refBook = refBookRepository.findByCode(refBookCode);
        return refBook != null;
    }

    private RefBook refBookModel(RefBookVersionEntity entity, List<RefBookVersionEntity> lastPublishVersions) {
        if (entity == null) return null;
        RefBook model = new RefBook(ModelGenerator.versionModel(entity));
        model.setStatus(entity.getStatus());
        model.setRemovable(isRefBookRemovable(entity.getRefBook().getId()));
        model.setCategory(entity.getRefBook().getCategory());
        Optional<RefBookVersionEntity> lastPublishedVersion = lastPublishVersions.stream().filter(v -> v.getRefBook().getId().equals(entity.getRefBook().getId())).findAny();
        model.setLastPublishedVersionFromDate(lastPublishedVersion.map(RefBookVersionEntity::getFromDate).orElse(null));
        model.setLastPublishedVersion(lastPublishedVersion.map(RefBookVersionEntity::getVersion).orElse(null));
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
        passportValueRepository.delete(toRemove);
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

    private List<RefBookVersionEntity> getLastPublishedVersions(List<Integer> refBookIds) {
        RefBookCriteria lastPublishVersionCriteria = new RefBookCriteria();
        lastPublishVersionCriteria.setRefBookInfo(RefBookInfo.PUBLISHED);
        lastPublishVersionCriteria.setRefBookIds(refBookIds);
        return repository.findAll(toPredicate(lastPublishVersionCriteria),
                new PageRequest(0, refBookIds.size())).getContent();
    }

    private void validateRefBookExists(Integer refBookId) {
        if (refBookId == null || !refBookRepository.exists(refBookId)) {
            throw new NotFoundException(new Message("refbook.not.found", refBookId));
        }
    }

    private void validateVersionExists(Integer versionId) {
        if (versionId == null || !repository.exists(versionId)) {
            throw new NotFoundException(new Message("version.not.found", versionId));
        }
    }

    private void validateVersionNotArchived(Integer versionId) {
        if (versionId != null && repository.exists(hasVersionId(versionId).and(isArchived()))) {
            throw new UserException("refbook.is.archived");
        }
    }

}
