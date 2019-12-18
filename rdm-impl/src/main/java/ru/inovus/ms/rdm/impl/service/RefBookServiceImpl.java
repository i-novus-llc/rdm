package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.inovus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.api.model.refdata.ChangeDataRequest;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.*;
import ru.inovus.ms.rdm.impl.file.FileStorage;
import ru.inovus.ms.rdm.impl.file.process.XmlCreateRefBookFileProcessor;
import ru.inovus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.inovus.ms.rdm.impl.repository.PassportValueRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookModelDataRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.util.ModelGenerator;
import ru.inovus.ms.rdm.impl.util.NamingUtils;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.*;

@Primary
@Service
public class RefBookServiceImpl implements RefBookService {

    private static final String REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE = "refbook.already.exists";

    private RefBookRepository refBookRepository;
    private RefBookVersionRepository versionRepository;
    private RefBookModelDataRepository refBookModelDataRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;

    private RefBookLockService refBookLockService;

    private PassportValueRepository passportValueRepository;
    private RefBookVersionQueryProvider refBookVersionQueryProvider;

    private VersionValidation versionValidation;

    private AuditLogService auditLogService;

    private FileStorage fileStorage;

    private DraftService draftService;
    private PublishService publishService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public RefBookServiceImpl(RefBookRepository refBookRepository, RefBookVersionRepository versionRepository,
                              RefBookModelDataRepository refBookModelDataRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              RefBookLockService refBookLockService,
                              PassportValueRepository passportValueRepository, RefBookVersionQueryProvider refBookVersionQueryProvider,
                              VersionValidation versionValidation, AuditLogService auditLogService, FileStorage fileStorage, DraftService draftService,
                              PublishService publishService) {
        this.refBookRepository = refBookRepository;
        this.versionRepository = versionRepository;

        this.refBookModelDataRepository = refBookModelDataRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;

        this.passportValueRepository = passportValueRepository;
        this.refBookVersionQueryProvider = refBookVersionQueryProvider;

        this.versionValidation = versionValidation;
        this.auditLogService = auditLogService;
        this.fileStorage = fileStorage;

        this.draftService = draftService;
        this.publishService = publishService;
    }

    /**
     * Поиск справочников по критерию.
     *
     * @param criteria критерий поиска
     * @return Список сущностей
     */
    @Override
    @Transactional
    public Page<RefBook> search(RefBookCriteria criteria) {

        Page<RefBookVersionEntity> entities = refBookVersionQueryProvider.search(criteria);

        List<Integer> refBookIds = entities.getContent().stream()
                .map(v -> v.getRefBook().getId())
                .collect(toList());

        return entities.map(entity ->
                refBookModel(entity,
                        criteria.getExcludeDraft() ? emptyList() : getSourceTypeVersions(refBookIds, RefBookSourceType.DRAFT),
                        getSourceTypeVersions(refBookIds, RefBookSourceType.LAST_PUBLISHED))
        );
    }

    /**
     * Поиск версий справочников по критерию.
     *
     * @param criteria критерий поиска
     * @return Список сущностей
     */
    @Override
    @Transactional
    public Page<RefBook> searchVersions(RefBookCriteria criteria) {

        criteria.setIncludeVersions(true);

        return search(criteria);
    }

    @Override
    @Transactional
    public RefBook getByVersionId(Integer versionId) {

        versionValidation.validateVersionExists(versionId);

        RefBookVersionEntity version = versionRepository.getOne(versionId);

        boolean hasReferrerVersions = hasReferrerVersions(version.getRefBook().getCode());

        return refBookModel(version, hasReferrerVersions,
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
        if (refBookEntity == null) {
            throw new NotFoundException();
        }
        return refBookEntity.getId();
    }

    @Override
    @Transactional
    public RefBook create(RefBookCreateRequest request) {

        if (refBookRepository.existsByCode(request.getCode()))
            throw new UserException(new Message(REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE, request.getCode()));
        NamingUtils.checkCode(request.getCode());
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

        String storageCode = draftDataService.createDraft(emptyList());
        refBookVersionEntity.setStorageCode(storageCode);
        Structure structure = new Structure();
        structure.setAttributes(emptyList());
        structure.setReferences(emptyList());
        refBookVersionEntity.setStructure(structure);

        RefBookVersionEntity savedVersion = versionRepository.save(refBookVersionEntity);
        RefBook refBook = refBookModel(savedVersion, false,
            getSourceTypeVersion(savedVersion.getRefBook().getId(), RefBookSourceType.DRAFT),
            getSourceTypeVersion(savedVersion.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED)
        );
        auditLogService.addAction(
            AuditAction.CREATE_REF_BOOK,
            () -> savedVersion
        );
        return refBook;
    }

    @Override
    public Draft create(FileModel fileModel) {
        String extension = FilenameUtils.getExtension(fileModel.getName()).toUpperCase();
        switch (extension) {
            case "XLSX": return createByXlsx(fileModel);
            case "XML": return createByXml(fileModel);
            default: throw new UserException("file.extension.invalid");
        }
    }

    @SuppressWarnings("unused")
    private Draft createByXlsx(FileModel fileModel) {
        throw new UserException("xlsx.draft.creation.not-supported");
    }

    private Draft createByXml(FileModel fileModel) {
        Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());
        RefBook refBook;
        try (XmlCreateRefBookFileProcessor createRefBookFileProcessor = new XmlCreateRefBookFileProcessor(this)) {
            refBook = createRefBookFileProcessor.process(inputStreamSupplier);
        }
        return draftService.create(refBook.getRefBookId(), fileModel);
    }

    @Override
    @Transactional
    public RefBook update(RefBookUpdateRequest request) {

        versionValidation.validateVersion(request.getVersionId());
        refBookLockService.validateRefBookNotBusyByVersionId(request.getVersionId());

        RefBookVersionEntity versionEntity = versionRepository.getOne(request.getVersionId());
        RefBookEntity refBookEntity = versionEntity.getRefBook();
        if (!refBookEntity.getCode().equals(request.getCode())) {
            NamingUtils.checkCode(refBookEntity.getCode());
            if (refBookRepository.existsByCode((request.getCode())))
                throw new UserException(new Message(REF_BOOK_ALREADY_EXISTS_EXCEPTION_CODE, request.getCode()));

            refBookEntity.setCode(request.getCode());
        }
        refBookEntity.setCategory(request.getCategory());
        updateVersionFromPassport(versionEntity, request.getPassport());
        versionEntity.setComment(request.getComment());
        auditLogService.addAction(
                AuditAction.EDIT_PASSPORT,
                () -> versionEntity,
                Map.of("newPassport", request.getPassport())
        );
        return refBookModel(versionEntity, false,
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.DRAFT),
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED));
    }

    @Override
    @Transactional
    public void delete(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);
        refBookLockService.validateRefBookNotBusyByRefBookId(refBookId);

        // NB: may-be: Move to `RefBookVersionQueryProvider`.
        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        List<RefBookVersionEntity> l = refBookEntity.getVersionList();
        RefBookVersionEntity last = null;
        for (RefBookVersionEntity e : l) {
            if (last == null || last.getId() < e.getId())
                last = e;
        }
//      Подтягиваем из базы данные о пасспорте,
//      потому что их уже не будет там после удаления (fetchType по дефолту -- LAZY)
        if (last != null) {
            last.getPassportValues().forEach(PassportValueEntity::getAttribute);
            last.setRefBook(refBookEntity);
        }
        l.forEach(v ->
                dropDataService.drop(refBookRepository.getOne(refBookId).getVersionList().stream()
                        .map(RefBookVersionEntity::getStorageCode)
                        .collect(Collectors.toSet())));
        refBookRepository.deleteById(refBookId);
        if (last != null) {
            RefBookVersionEntity finalLast = last;
            auditLogService.addAction(
                AuditAction.DELETE_REF_BOOK,
                () -> finalLast
            );
        }
    }

    @Override
    @Transactional
    public void toArchive(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);

        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        // NB: Add checking references to this refBook.
        refBookEntity.setArchived(Boolean.TRUE);
        RefBookVersionEntity last = null;
        for (RefBookVersionEntity e : refBookEntity.getVersionList()) {
            if (last == null || e.getId() > last.getId())
                last = e;
        }
        refBookRepository.save(refBookEntity);
        RefBookVersionEntity finalLast = last;
        auditLogService.addAction(
            AuditAction.ARCHIVE,
            () -> finalLast
        );
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
    @SuppressWarnings("squid:S2259")
    public void changeData(ChangeDataRequest request) {
        RefBookEntity refBook = refBookRepository.findByCode(request.getRefBookCode());
        versionValidation.validateRefBookExists(refBook == null ? null : refBook.getId());
        refBookLockService.setRefBookUploading(refBook.getId());
        try {
            Integer draftId = draftService.getIdByRefBookCode(request.getRefBookCode());
            if (draftId == null) {
                RefBookVersionEntity mostRecentVersion = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(request.getRefBookCode(), RefBookVersionStatus.PUBLISHED);
                Draft draft = draftService.createFromVersion(mostRecentVersion.getId());
                draftId = draft.getId();
            }
            if (draftId == null)
                throw new UserException(new Message("draft.not.found", draftId));
            draftService.updateData(draftId, request.getRowsToAddOrUpdate());
            draftService.deleteRows(draftId, draftService.getSystemIdsByPrimaryKey(draftId, request.getRowsToDelete()));
            publishService.publish(draftId, null, null, null, false);
        } finally {
            refBookLockService.deleteRefBookOperation(refBook.getId());
        }
    }

    private RefBook refBookModel(RefBookVersionEntity entity,
                                 List<RefBookVersionEntity> draftVersions, List<RefBookVersionEntity> lastPublishVersions) {
        if (entity == null) return null;

        RefBookVersionEntity draftVersion = getRefBookSourceTypeVersion(entity.getRefBook().getId(), draftVersions);
        RefBookVersionEntity lastPublishedVersion = getRefBookSourceTypeVersion(entity.getRefBook().getId(), lastPublishVersions);

        return refBookModel(entity, false, draftVersion, lastPublishedVersion);
    }

    private RefBook refBookModel(RefBookVersionEntity entity, boolean hasReferrerVersions,
                                 RefBookVersionEntity draftVersion, RefBookVersionEntity lastPublishedVersion) {
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

        model.setHasReferrer(hasReferrerVersions);

        RefBookModelData refBookModelData = refBookModelDataRepository.findData(
                model.getId(),
                lastPublishedVersion != null,
                lastPublishedVersion != null ? lastPublishedVersion.getId() : 0);

        model.setHasDataConflict(refBookModelData.getHasDataConflict());
        model.setHasUpdatedConflict(refBookModelData.getHasUpdatedConflict());
        model.setHasAlteredConflict(refBookModelData.getHasAlteredConflict());
        model.setHasStructureConflict(refBookModelData.getHasStructureConflict());
        model.setLastHasDataConflict(refBookModelData.getLastHasDataConflict());

        return model;
    }

    /** Проверка на наличие справочников, ссылающихся на указанный справочник. */
    private boolean hasReferrerVersions(String refBookCode) {

        PageRequest pageRequest = PageRequest.of(0, 1);
        Page<RefBookVersionEntity> entities = versionRepository.findReferrerVersions(refBookCode,
                RefBookStatusType.ALL.name(), RefBookSourceType.ALL.name(), pageRequest);

        return entities != null && !entities.getContent().isEmpty();
    }

    private boolean isRefBookRemovable(Integer refBookId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(refBookId));
        where.and(isRemovable().not().or(isArchived()).or(isPublished()));
        return (where.getValue() != null) && !versionRepository.exists(where.getValue());
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
                .filter(e -> newPassportValues.stream()
                        .anyMatch(v -> e.getKey().equals(v.getAttribute().getCode())))
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
        versionCriteria.setSourceType(refBookSourceType);
        versionCriteria.setRefBookIds(refBookIds);

        return versionRepository.findAll(refBookVersionQueryProvider.toPredicate(versionCriteria),
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
