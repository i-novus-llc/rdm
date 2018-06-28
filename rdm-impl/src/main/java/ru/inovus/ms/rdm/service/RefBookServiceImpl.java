package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;


@Service
@Qualifier("impl")
public class RefBookServiceImpl implements RefBookService {

    private RefBookVersionRepository repository;

    private RefBookRepository refBookRepository;

    private static final Logger logger = LoggerFactory.getLogger(RefBookServiceImpl.class);

    @Autowired
    public RefBookServiceImpl(RefBookVersionRepository repository, RefBookRepository refBookRepository) {
        this.repository = repository;
        this.refBookRepository = refBookRepository;
    }

    @Override
    public Page<RefBook> search(RefBookCriteria criteria) {
        Pageable pageable = new PageRequest(criteria.getPageNumber() - 1, criteria.getPageSize(), toSort(criteria));
        Page<RefBookVersionEntity> list = repository.findAll(toPredicate(criteria), pageable);
        return list.map(this::refBookModel);
    }

    @Override
    public Passport getById(Integer versionId) {
        return passportModel(repository.findOne(versionId));
    }

    @Override
    public RefBook create(RefBookCreateRequest request) {
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setArchived(Boolean.FALSE);
        refBookEntity.setRemovable(Boolean.TRUE);
        refBookEntity.setCode(request.getCode());

        RefBookVersionEntity refBookVersionEntity = new RefBookVersionEntity();
        refBookVersionEntity.populateFrom(request);
        refBookVersionEntity.setRefBook(refBookEntity);
        refBookVersionEntity.setStatus(RefBookVersionStatus.DRAFT);

        return refBookModel(repository.save(refBookVersionEntity));
    }

    @Override
    public RefBook update(RefBookUpdateRequest request) {
        RefBookVersionEntity refBookVersionEntity = repository.findOne(request.getId());
        refBookVersionEntity.getRefBook().setCode(request.getCode());
        refBookVersionEntity.populateFrom(request);
        return refBookModel(repository.save(refBookVersionEntity));
    }

    @Override
    public void delete(int refBookId) {
        // удалить наполнение
       refBookRepository.delete(refBookId);
    }

    @Override
    public void archive(int refBookId) {
        RefBookEntity refBookEntity = refBookRepository.findOne(refBookId);
        refBookEntity.setArchived(Boolean.TRUE);
        refBookRepository.save(refBookEntity);
    }

    @Override
    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {
        Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "fromDate", Sort.NullHandling.NULLS_FIRST));
        Predicate predicate = isVersionOfRefBook(criteria.getRefBookId()).and(isDraftExcluded(criteria.getExcludeDraft()));
        Pageable pageable = new PageRequest(criteria.getPageNumber() - 1, criteria.getPageSize(), sort);
        Page<RefBookVersionEntity> list = repository.findAll(predicate, pageable);
        return list.map(this::versionModel);
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

        if (!isEmpty(criteria.getName())) {
            where.and(isShortNameOrFullNameContains(criteria.getName()));
        }
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

    private Sort toSort(RefBookCriteria criteria) {
        List<Sort.Order> orders = criteria.getOrders();
        if (CollectionUtils.isEmpty(orders))
            orders = Collections.singletonList(new Sort.Order("refBook.code"));

        Sort sort = null;
        for (Sort.Order order : orders)
            if (sort == null) sort = sort(order);
            else sort.and(sort(order));
        return sort;
    }

    private Sort sort(Sort.Order order) {
        String property = order.getProperty();
        Sort.Direction direction = order.getDirection();

        if ("version".equals(property))
            return new Sort(direction, "refBook.archived").and(new Sort(direction, property));
        else
            return new Sort(direction, property);
    }

    private boolean hasPublishing(Integer refBookId) {
        return repository.exists(isVersionOfRefBook(refBookId).and(isPublishing()));
    }

    private RefBookVersionEntity getLastPublishedVersion(Integer refBookId) {
        return repository.findOne(isVersionOfRefBook(refBookId).and(isLastPublished()));
    }

    private RefBookVersion getFirstPublishedVersion(Integer refBookId) {
        VersionCriteria versionCriteria = new VersionCriteria();
        versionCriteria.setRefBookId(refBookId);
        versionCriteria.noPagination();
        versionCriteria.setExcludeDraft(Boolean.TRUE);
        Page<RefBookVersion> search = getVersions(versionCriteria);
        if (search.getTotalElements() == 0) return null;
        return search.getContent().get(search.getContent().size() - 1);
    }

    private boolean isRefBookRemovable(Integer refBookId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(refBookId));
        where.and(isRemovable().not().or(isArchived()).or(isPublished()).or(isPublishing()));
        return !repository.exists(where.getValue());
    }

    private boolean isActualVersion(LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDateTime now = LocalDateTime.now();
        return !isNull(fromDate)
                && !fromDate.isAfter(now)
                && fromDate.isBefore(now) && (isNull(toDate) || toDate.isAfter(now));
    }

    private String getDisplayVersion(RefBookVersionEntity entity) {
        if (entity.getRefBook().getArchived())
            return RefBookStatus.ARCHIVED.getName();
        if (RefBookVersionStatus.PUBLISHED.equals(entity.getStatus()))
            return entity.getVersion();
        return RefBookVersionStatus.DRAFT.getName();
    }

    private String getDisplayStatus(RefBookVersionEntity entity) {
        if (entity.getRefBook().getArchived())
            return RefBookStatus.ARCHIVED.name();
        if (RefBookVersionStatus.PUBLISHED.equals(entity.getStatus()))
            return isActualVersion(entity.getFromDate(), entity.getToDate()) ? entity.getStatus().name() : null;
        else
            return entity.getStatus().name();
    }

    private LocalDateTime getLastFromDate(RefBookVersionEntity entity) {
        if (nonNull(entity.getFromDate()))
            entity.getFromDate();
        RefBookVersionEntity lastPublishedVersion = getLastPublishedVersion(entity.getRefBook().getId());
        return nonNull(lastPublishedVersion) ? lastPublishedVersion.getFromDate() : null;
    }

    private RefBookVersion versionModel(RefBookVersionEntity entity) {
        if (entity == null) return null;
        RefBookVersion model = new RefBookVersion();
        model.setId(entity.getId());
        model.setRefBookId(entity.getRefBook().getId());
        model.setCode(entity.getRefBook().getCode());
        model.setShortName(entity.getShortName());
        model.setFullName(entity.getFullName());
        model.setAnnotation(entity.getAnnotation());
        model.setComment(entity.getComment());
        model.setVersion(entity.getVersion());
        model.setFromDate(entity.getFromDate());
        model.setToDate(entity.getToDate());
        model.setArchived(entity.getRefBook().getArchived());
        model.setStatus(entity.getStatus());
        model.setRefBookHasPublishingVersion(hasPublishing(entity.getRefBook().getId()));
        model.setDisplayStatus(getDisplayStatus(entity));
        return model;
    }

    private RefBook refBookModel(RefBookVersionEntity entity) {
        if (entity == null) return null;
        RefBook model = new RefBook(versionModel(entity));
        model.setStatus(entity.getStatus());
        model.setRemovable(isRefBookRemovable(entity.getRefBook().getId()));
        model.setDisplayVersion(getDisplayVersion(entity));
        model.setFromDate(getLastFromDate(entity));
        return model;
    }

    private Passport passportModel(RefBookVersionEntity entity) {
        if (entity == null) return null;
        Passport model = new Passport(refBookModel(entity));
        RefBookVersion firstPublishedVersion = getFirstPublishedVersion(entity.getRefBook().getId());
        model.setFirstPublishedVersionFromDate(nonNull(firstPublishedVersion) ? firstPublishedVersion.getFromDate() : null);
        // setRecordsCount
        return model;
    }
}
