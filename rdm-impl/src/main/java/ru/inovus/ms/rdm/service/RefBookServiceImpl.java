package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
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
import ru.inovus.ms.rdm.repositiory.PassportValueRepository;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.util.ModelGenerator;

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

    private static final Logger logger = LoggerFactory.getLogger(RefBookServiceImpl.class);
    private RefBookVersionRepository repository;
    private RefBookRepository refBookRepository;
    private DraftDataService draftDataService;
    private DropDataService dropDataService;
    private PassportValueRepository passportValueRepository;

    @Autowired
    public RefBookServiceImpl(RefBookVersionRepository repository, RefBookRepository refBookRepository,
                              DraftDataService draftDataService, DropDataService dropDataService, PassportValueRepository passportValueRepository) {
        this.repository = repository;
        this.refBookRepository = refBookRepository;
        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;
        this.passportValueRepository = passportValueRepository;
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
        Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "fromDate", Sort.NullHandling.NULLS_FIRST));
        Pageable pageable = new PageRequest(criteria.getPageNumber() - 1, criteria.getPageSize(), sort);
        Page<RefBookVersionEntity> list = repository.findAll(toPredicate(criteria), pageable);
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
            criteria.getPassport()
                    .forEach((k, v) -> where.and(hasAttributeValue(k, v!=null ? v.getValue() : null)));        }

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

    private void populateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, PassportAttributeValue> passport) {
        if (passport != null && versionEntity != null) {
            versionEntity.setPassportValues(passport.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue().getValue() != null)
                    .map(e -> new PassportValueEntity(new PassportAttributeEntity(e.getKey()), e.getValue().getValue(), versionEntity))
                    .collect(Collectors.toSet()));
        }
    }

    private void updeteVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, PassportAttributeValue> newPassport) {
        if (newPassport == null || versionEntity == null) {
            return;
        }

        Set<PassportValueEntity> newPassportValues = versionEntity.getPassportValues() != null ?
                versionEntity.getPassportValues() : new HashSet<>();

        Map<String, PassportAttributeValue> correctUpdatePassport = new HashMap<>(newPassport);

        Set<String> attributeCodesToRemove = correctUpdatePassport.entrySet().stream()
                .filter(e -> e.getValue() == null || e.getValue().getValue() == null)
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
                        .findAny().get().setValue(e.getValue().getValue()))
                .collect(Collectors.toSet());
        correctUpdatePassport.entrySet().removeAll(toUpdate);

        newPassportValues.addAll(correctUpdatePassport.entrySet().stream()
                .map(a -> new PassportValueEntity(new PassportAttributeEntity(a.getKey()), a.getValue().getValue(), versionEntity))
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
