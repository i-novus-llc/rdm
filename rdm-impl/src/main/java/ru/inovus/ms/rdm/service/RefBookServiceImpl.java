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
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

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

    private static final Logger logger = LoggerFactory.getLogger(RefBookServiceImpl.class);

    @Autowired
    public RefBookServiceImpl(RefBookVersionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<RefBook> search(RefBookCriteria criteria) {
        Pageable pageable = new PageRequest(criteria.getPageNumber() - 1, criteria.getPageSize(), toSort(criteria));
        Page<RefBookVersionEntity> list = repository.findAll(toPredicate(criteria), pageable);
        return list.map(this::model);
    }

    @Override
    public RefBook getById(Integer versionId) {
        return model(repository.findOne(versionId));
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

        return model(repository.save(refBookVersionEntity));
    }

    @Override
    public Page<RefBookVersion> getVersions(String refBookId) {
        throw new UnsupportedOperationException();
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

    private RefBookVersionEntity getLastPublishedVersion(Integer refBookId) {
        return repository.findOne(isVersionOfRefBook(refBookId).and(isLastPublished()));
    }

    private boolean isRefBookRemovable(Integer refBookId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(refBookId));
        where.and(isRemovable().not().or(isArchived()).or(isPublished()).or(isPublishing()));
        return !repository.exists(where.getValue());
    }

    private RefBook model(RefBookVersionEntity entity) {
        if (entity == null) return null;
        RefBook model = new RefBook();
        model.setId(entity.getId());
        model.setRefBookId(entity.getRefBook().getId());
        model.setCode(entity.getRefBook().getCode());
        model.setFullName(entity.getFullName());
        model.setShortName(entity.getShortName());
        model.setVersion(entity.getVersion());
        model.setAnnotation(entity.getAnnotation());
        model.setStatus(entity.getStatus());
        model.setComment(entity.getComment());
        model.setArchived(entity.getRefBook().getArchived());
        model.setRemovable(isRefBookRemovable(entity.getRefBook().getId()));
        if (RefBookVersionStatus.DRAFT.equals(entity.getStatus()) || RefBookVersionStatus.PUBLISHING.equals(entity.getStatus()))
            model.setVersion(RefBookStatus.DRAFT.getName());
        if (entity.getRefBook().getArchived())
            model.setVersion(RefBookStatus.ARCHIVED.getName());
        if (isNull(model.getFromDate())) {
            RefBookVersionEntity lastPublishedVersion = getLastPublishedVersion(entity.getRefBook().getId());
            model.setFromDate(nonNull(lastPublishedVersion) ? lastPublishedVersion.getFromDate() : null);
        } else {
            model.setFromDate(entity.getFromDate());
        }
        return model;
    }
}
