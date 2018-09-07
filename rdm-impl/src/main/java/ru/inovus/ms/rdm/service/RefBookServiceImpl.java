package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.PassportValueRepository;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.util.ModelGenerator;
import ru.inovus.ms.rdm.util.PassportPredicateProducer;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

@Primary
@Service
public class RefBookServiceImpl implements RefBookService {

    private static final String PASSPORT_SORT_PREFIX = "passport";
    private static final String VERSION_ID_SORT_PROPERTY = "id";
    private static final String REF_BOOK_ID_SORT_PROPERTY = "refbookId";
    private static final String REF_BOOK_CODE_SORT_PROPERTY = "code";

    private static final Logger logger = LoggerFactory.getLogger(RefBookServiceImpl.class);
    private RefBookVersionRepository repository;
    private RefBookRepository refBookRepository;
    private DraftDataService draftDataService;
    private DropDataService dropDataService;
    private PassportValueRepository passportValueRepository;
    private PassportPredicateProducer passportPredicateProducer;
    private EntityManager entityManager;

    @Autowired
    public RefBookServiceImpl(RefBookVersionRepository repository, RefBookRepository refBookRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              PassportValueRepository passportValueRepository, PassportPredicateProducer passportPredicateProducer, EntityManager entityManager) {
        this.repository = repository;
        this.refBookRepository = refBookRepository;
        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
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
        return list.map(this::refBookModel);
    }

    private void sortQuery(JPAQuery<RefBookVersionEntity> jpaQuery, RefBookCriteria criteria) {
        List<Sort.Order> orders = criteria.getOrders();

        if (CollectionUtils.isEmpty(orders)) {
            jpaQuery.orderBy(QRefBookVersionEntity.refBookVersionEntity.fromDate.asc());
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
                default:
                    throw new UserException(new Message("cannot.order.by", order.getProperty()));
            }
        }
        jpaQuery.orderBy(order.isAscending() ? sortExpression.asc() : sortExpression.desc());
    }

    @Override
    @Transactional
    public RefBook getByVersionId(Integer versionId) {

        validateVersionExists(versionId);

        return refBookModel(repository.findOne(versionId));
    }

    @Override
    @Transactional
    public RefBook create(RefBookCreateRequest request) {
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setArchived(Boolean.FALSE);
        refBookEntity.setRemovable(Boolean.TRUE);
        refBookEntity.setCode(request.getCode());
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

        return refBookModel(repository.save(refBookVersionEntity));
    }

    @Override
    @Transactional
    public RefBook update(RefBookUpdateRequest request) {

        validateVersionExists(request.getId());
        validateVersionNotArchived(request.getId());

        RefBookVersionEntity refBookVersionEntity = repository.findOne(request.getId());
        RefBookEntity refBookEntity = refBookVersionEntity.getRefBook();
        if (!refBookEntity.getCode().equals(request.getCode())) {
            refBookEntity.setCode(request.getCode());
        }
        updateVersionFromPassport(refBookVersionEntity, request.getPassport());
        refBookVersionEntity.setComment(request.getComment());
        return refBookModel(refBookVersionEntity);
    }

    @Override
    @Transactional
    public void delete(int refBookId) {
        refBookRepository.getOne(refBookId).getVersionList().forEach(v ->
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
        criteria.setOrders(Collections.singletonList(new Sort.Order(Sort.Direction.DESC, "fromDate", Sort.NullHandling.NULLS_FIRST)));
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

        where.and(isLast());

        if (nonNull(criteria.getFromDateBegin()))
            where.and(isMaxFromDateEqOrAfter(criteria.getFromDateBegin()));

        if (nonNull(criteria.getFromDateEnd()))
            where.and(isMaxFromDateEqOrBefore(criteria.getFromDateEnd()));

        if (!isEmpty(criteria.getCode()))
            where.and(isCodeContains(criteria.getCode()));

        if (!isEmpty(criteria.getPassport())) {
            where.and(passportPredicateProducer.toPredicate(criteria.getPassport()));
        }

        if (!isEmpty(criteria.getRefBookId()))
            where.and(isVersionOfRefBook(criteria.getRefBookId()));

        if (nonNull(criteria.getStatus())) {
            switch (criteria.getStatus()) {
                case PUBLISHED:
                    where.andNot(isArchived()).and(isAnyPublished());
                    break;
                case DRAFT:
                    where.andNot(isArchived()).and(isDraft().or(isPublishing()));
                    break;
                case ARCHIVED:
                    where.and(isArchived());
                    break;
                default:
                    logger.debug("No filters for status: {}", criteria.getStatus().getName());
            }
        }
        return where.getValue();
    }

    private RefBookVersionEntity getLastPublishedVersion(Integer refBookId) {
        return repository.findOne(isVersionOfRefBook(refBookId).and(isLastPublished()));
    }

    private LocalDateTime getLastPublishedVersionFromDate(RefBookVersionEntity entity) {
        if (nonNull(entity.getFromDate()))
            entity.getFromDate();
        RefBookVersionEntity lastPublishedVersion = getLastPublishedVersion(entity.getRefBook().getId());
        return nonNull(lastPublishedVersion) ? lastPublishedVersion.getFromDate() : null;
    }

    private boolean isRefBookRemovable(Integer refBookId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(refBookId));
        where.and(isRemovable().not().or(isArchived()).or(isPublished()).or(isPublishing()));
        return !repository.exists(where.getValue());
    }

    private String getDisplayVersion(RefBookVersionEntity entity) {
        if (entity.getRefBook().getArchived())
            return RefBookStatus.ARCHIVED.getName();
        if (RefBookVersionStatus.PUBLISHED.equals(entity.getStatus()))
            return entity.getVersion();
        return RefBookVersionStatus.DRAFT.getName();
    }

    private RefBook refBookModel(RefBookVersionEntity entity) {
        if (entity == null) return null;
        RefBook model = new RefBook(ModelGenerator.versionModel(entity));
        model.setStatus(entity.getStatus());
        model.setRemovable(isRefBookRemovable(entity.getRefBook().getId()));
        model.setDisplayVersion(getDisplayVersion(entity));
        model.setLastPublishedVersionFromDate(getLastPublishedVersionFromDate(entity));
        return model;
    }

    private void populateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, String> passport) {
        if (passport != null && versionEntity != null) {
            versionEntity.setPassportValues(passport.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> new PassportValueEntity(new PassportAttributeEntity(e.getKey()), e.getValue(), versionEntity))
                    .collect(Collectors.toSet()));
        }
    }

    private void updateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, String> newPassport) {
        if (newPassport == null || versionEntity == null) {
            return;
        }

        Set<PassportValueEntity> newPassportValues = versionEntity.getPassportValues() != null ?
                versionEntity.getPassportValues() : new HashSet<>();

        Map<String, String> correctUpdatePassport = new HashMap<>(newPassport);

        Set<String> attributeCodesToRemove = correctUpdatePassport.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        List<PassportValueEntity> toRemove = versionEntity.getPassportValues().stream()
                .filter(v -> attributeCodesToRemove.contains(v.getAttribute().getCode()))
                .collect(Collectors.toList());
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
                .collect(Collectors.toList()));

        versionEntity.setPassportValues(newPassportValues);
    }

    private void validateRefBookExists(Integer refBookId) {
        if (refBookId == null || !refBookRepository.exists(refBookId)) {
            throw new UserException("refbook.not.found");
        }
    }

    private void validateVersionExists(Integer versionId) {
        if (versionId == null || !repository.exists(versionId)) {
            throw new UserException("version.not.found");
        }
    }

    private void validateVersionNotArchived(Integer versionId) {
        if (versionId != null && repository.exists(hasVersionId(versionId).and(isArchived()))) {
            throw new UserException("refbook.is.archived");
        }
    }

}
