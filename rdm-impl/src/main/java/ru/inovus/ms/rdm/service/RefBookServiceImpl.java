package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.util.TimeUtils;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

@Service
public class RefBookServiceImpl implements RefBookService {

    private static final Logger logger = LoggerFactory.getLogger(RefBookServiceImpl.class);
    private RefBookVersionRepository repository;
    private RefBookRepository refBookRepository;
    private DraftDataService draftDataService;
    private DropDataService dropDataService;

    @Autowired
    public RefBookServiceImpl(RefBookVersionRepository repository, RefBookRepository refBookRepository,
                              DraftDataService draftDataService, DropDataService dropDataService) {
        this.repository = repository;
        this.refBookRepository = refBookRepository;
        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
    }

    @Override
    @Transactional
    public Page<RefBook> search(RefBookCriteria criteria) {
        Pageable pageable = new PageRequest(criteria.getPageNumber() - 1, criteria.getPageSize(), toSort(criteria));
        Page<RefBookVersionEntity> list = repository.findAll(toPredicate(criteria), pageable);
        return list.map(this::refBookModel);
    }

    @Override
    @Transactional
    public RefBook getById(Integer versionId) {
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
        refBookVersionEntity.setStructure(structure);

        return refBookModel(repository.save(refBookVersionEntity));
    }

    @Override
    @Transactional
    public RefBook update(RefBookUpdateRequest request) {
        RefBookVersionEntity refBookVersionEntity = repository.findOne(request.getId());
        RefBookEntity refBookEntity = refBookVersionEntity.getRefBook();
        if (!refBookEntity.getCode().equals(request.getCode())) {
            refBookEntity.setCode(request.getCode());
        }
        updeteVersionFromPassport(refBookVersionEntity, request.getPassport());
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
    public void archive(int refBookId) {
        RefBookEntity refBookEntity = refBookRepository.findOne(refBookId);
        refBookEntity.setArchived(Boolean.TRUE);
        refBookRepository.save(refBookEntity);
    }

    @Override
    @Transactional
    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {
        Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "fromDate", Sort.NullHandling.NULLS_FIRST));
        Pageable pageable = new PageRequest(criteria.getPageNumber() - 1, criteria.getPageSize(), sort);
        Page<RefBookVersionEntity> list = repository.findAll(toPredicate(criteria), pageable);
        return list.map(this::versionModel);
    }

    private Predicate toPredicate(VersionCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(criteria.getRefBookId()));
        if (criteria.getExcludeDraft())
            where.andNot(isDraft());
        if(nonNull(criteria.getVersion()))
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

        if (!isEmpty(criteria.getPassport())){
            criteria.getPassport().getAttributes().entrySet()
                    .forEach(e -> where.and(hasAttributeValue(e.getKey(), e.getValue())));
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

    private LocalDateTime getLastPublishedVersionFromDate(RefBookVersionEntity entity) {
        if (nonNull(entity.getFromDate()))
            entity.getFromDate();
        RefBookVersionEntity lastPublishedVersion = getLastPublishedVersion(entity.getRefBook().getId());
        return nonNull(lastPublishedVersion) ? lastPublishedVersion.getFromDate() : null;
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

    private boolean isActualVersion(RefBookVersionEntity entity) {
        return TimeUtils.isSameOrBeforeNow(entity.getFromDate()) && TimeUtils.isNullOrAfterNow(entity.getToDate());
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
            return isActualVersion(entity) ? entity.getStatus().name() : null;
        else
            return entity.getStatus().name();
    }

    private RefBookVersion versionModel(RefBookVersionEntity entity) {
        if (entity == null) return null;
        RefBookVersion model = new RefBookVersion();
        model.setId(entity.getId());
        model.setRefBookId(entity.getRefBook().getId());
        model.setCode(entity.getRefBook().getCode());
        model.setComment(entity.getComment());
        model.setVersion(entity.getVersion());
        model.setFromDate(entity.getFromDate());
        model.setToDate(entity.getToDate());
        model.setArchived(entity.getRefBook().getArchived());
        model.setStatus(entity.getStatus());
        model.setRefBookHasPublishingVersion(hasPublishing(entity.getRefBook().getId()));
        model.setDisplayStatus(getDisplayStatus(entity));
        Map<String, String> passport = new HashMap<>();
        if (entity.getPassportValues() != null)
            entity.getPassportValues().forEach(value -> passport.put(value.getAttribute().getCode(), value.getValue()));
        model.setPassport(new Passport(passport));
        return model;
    }

    private RefBook refBookModel(RefBookVersionEntity entity) {
        if (entity == null) return null;
        RefBook model = new RefBook(versionModel(entity));
        model.setStatus(entity.getStatus());
        model.setRemovable(isRefBookRemovable(entity.getRefBook().getId()));
        model.setDisplayVersion(getDisplayVersion(entity));
        model.setLastPublishedVersionFromDate(getLastPublishedVersionFromDate(entity));
        return model;
    }

    private void populateVersionFromPassport(RefBookVersionEntity versionEntity, Passport passport) {
        if (passport != null && passport.getAttributes() != null && versionEntity != null) {
            versionEntity.setPassportValues(passport.getAttributes().entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(e -> new PassportValueEntity(new PassportAttributeEntity(e.getKey()), e.getValue(), versionEntity))
                    .collect(Collectors.toSet()));
        }
    }

    private void updeteVersionFromPassport(RefBookVersionEntity versionEntity, Passport passport){
        if (passport == null || passport.getAttributes() == null || versionEntity == null) {
            return;
        }

        Map<String, String> newPassport = passport.getAttributes();

        Set<PassportValueEntity> newPassportValues = versionEntity.getPassportValues() != null ?
                versionEntity.getPassportValues() : new HashSet<>();

        newPassportValues.removeIf(v -> newPassport.keySet().contains(v.getAttribute().getCode())
                && newPassport.get(v.getAttribute().getCode()) == null);
        newPassportValues.forEach(v -> v.setValue(newPassport.get(v.getAttribute().getCode())));

        Set<String> existAttributes = newPassportValues.stream()
                .map(v -> v.getAttribute().getCode()).collect(Collectors.toSet());

        newPassportValues.addAll(newPassport.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .filter(e -> !existAttributes.contains(e.getKey()))
                .map(e -> new PassportValueEntity(new PassportAttributeEntity(e.getKey()), e.getValue(), versionEntity))
                .collect(Collectors.toSet()));

        versionEntity.setPassportValues(newPassportValues);
    }

}
