package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.api.enumeration.*;
import ru.inovus.ms.rdm.api.exception.FileExtensionException;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;
import ru.inovus.ms.rdm.api.model.refbook.*;
import ru.inovus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.inovus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.*;
import ru.inovus.ms.rdm.impl.file.FileStorage;
import ru.inovus.ms.rdm.impl.file.process.XmlCreateRefBookFileProcessor;
import ru.inovus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.inovus.ms.rdm.impl.repository.*;
import ru.inovus.ms.rdm.impl.util.FileUtil;
import ru.inovus.ms.rdm.impl.util.ModelGenerator;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity.stringPassportToValues;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.*;

@Primary
@Service
public class RefBookServiceImpl implements RefBookService {

    private static final String REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE = "refbook.is.not.created";
    private static final String REFBOOK_IS_NOT_CREATED_FROM_XLSX_EXCEPTION_CODE = "refbook.is.not.created.from.xlsx";
    private static final String REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE = "refbook.draft.not.found";
    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private RefBookRepository refBookRepository;
    private RefBookVersionRepository versionRepository;
    private RefBookModelDataRepository refBookModelDataRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;

    private RefBookLockService refBookLockService;

    private PassportValueRepository passportValueRepository;
    private RefBookVersionQueryProvider refBookVersionQueryProvider;

    private VersionValidation versionValidation;

    private FileStorage fileStorage;

    private DraftService draftService;
    private PublishService publishService;

    private AuditLogService auditLogService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public RefBookServiceImpl(RefBookRepository refBookRepository, RefBookVersionRepository versionRepository,
                              RefBookModelDataRepository refBookModelDataRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              RefBookLockService refBookLockService,
                              PassportValueRepository passportValueRepository, RefBookVersionQueryProvider refBookVersionQueryProvider,
                              VersionValidation versionValidation, FileStorage fileStorage,
                              DraftService draftService, PublishService publishService,
                              AuditLogService auditLogService) {
        this.refBookRepository = refBookRepository;
        this.versionRepository = versionRepository;

        this.refBookModelDataRepository = refBookModelDataRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;

        this.passportValueRepository = passportValueRepository;
        this.refBookVersionQueryProvider = refBookVersionQueryProvider;

        this.versionValidation = versionValidation;
        this.fileStorage = fileStorage;

        this.draftService = draftService;
        this.publishService = publishService;

        this.auditLogService = auditLogService;
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
        List<Integer> refBookIds = entities.getContent().stream().map(v -> v.getRefBook().getId()).collect(toList());

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

        versionValidation.validateRefBookCodeExists(refBookCode);
        return refBookRepository.findByCode(refBookCode).getId();
    }

    @Override
    @Transactional
    public RefBook create(RefBookCreateRequest request) {

        final String newCode = request.getCode();
        versionValidation.validateRefBookCode(newCode);
        versionValidation.validateRefBookCodeNotExists(newCode);

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setCode(newCode);
        refBookEntity.setArchived(Boolean.FALSE);
        refBookEntity.setRemovable(Boolean.TRUE);
        refBookEntity.setCategory(request.getCategory());
        refBookEntity = refBookRepository.save(refBookEntity);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setStatus(RefBookVersionStatus.DRAFT);

        if (request.getPassport() != null) {
            versionEntity.setPassportValues(stringPassportToValues(request.getPassport(), false, versionEntity));
        }

        String storageCode = draftDataService.createDraft(emptyList());
        versionEntity.setStorageCode(storageCode);
        Structure structure = new Structure();
        structure.setAttributes(emptyList());
        structure.setReferences(emptyList());
        versionEntity.setStructure(structure);

        RefBookVersionEntity savedEntity = versionRepository.save(versionEntity);
        RefBook refBook = refBookModel(savedEntity, false,
            getSourceTypeVersion(savedEntity.getRefBook().getId(), RefBookSourceType.DRAFT),
            getSourceTypeVersion(savedEntity.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED)
        );

        auditLogService.addAction(AuditAction.CREATE_REF_BOOK, () -> savedEntity);

        return refBook;
    }

    @Override
    @Transactional(timeout = 1200000)
    public Draft create(FileModel fileModel) {

        switch (FileUtil.getExtension(fileModel.getName())) {
            case "XLSX": return createByXlsx(fileModel);
            case "XML": return createByXml(fileModel);
            default: throw new FileExtensionException();
        }
    }

    @SuppressWarnings("unused")
    private Draft createByXlsx(FileModel fileModel) {
        throw new UserException(REFBOOK_IS_NOT_CREATED_FROM_XLSX_EXCEPTION_CODE);
    }

    private Draft createByXml(FileModel fileModel) {

        RefBook refBook;
        try (XmlCreateRefBookFileProcessor createRefBookFileProcessor = new XmlCreateRefBookFileProcessor(this)) {
            Supplier<InputStream> inputStreamSupplier = () -> fileStorage.getContent(fileModel.getPath());
            refBook = createRefBookFileProcessor.process(inputStreamSupplier);
        }

        if (refBook == null)
            throw new UserException(REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE);

        try {
            return draftService.create(refBook.getRefBookId(), fileModel);

        } catch (Exception e) {
            delete(refBook.getRefBookId());

            throw e;
        }
    }

    @Override
    @Transactional
    public RefBook update(RefBookUpdateRequest request) {

        final Integer versionId = request.getVersionId();
        versionValidation.validateVersion(versionId);
        refBookLockService.validateRefBookNotBusyByVersionId(versionId);

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        versionValidation.validateOptLockValue(versionId, versionEntity.getOptLockValue(), request.getOptLockValue());

        RefBookEntity refBookEntity = versionEntity.getRefBook();

        final String newCode = request.getCode();
        if (!refBookEntity.getCode().equals(newCode)) {
            versionValidation.validateRefBookCode(newCode);
            versionValidation.validateRefBookCodeNotExists(newCode);

            refBookEntity.setCode(newCode);
        }

        refBookEntity.setCategory(request.getCategory());
        updateVersionFromPassport(versionEntity, request.getPassport());
        versionEntity.setComment(request.getComment());

        forceUpdateVersionOptLockValue(versionEntity);

        auditLogService.addAction(AuditAction.EDIT_PASSPORT, () -> versionEntity, Map.of("newPassport", request.getPassport()));

        return refBookModel(versionEntity, false,
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.DRAFT),
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED));
    }

    @Override
    @Transactional
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void delete(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);
        refBookLockService.validateRefBookNotBusyByRefBookId(refBookId);

        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        List<RefBookVersionEntity> refBookVersions = refBookEntity.getVersionList();
        RefBookVersionEntity lastVersion = getLastVersion(refBookVersions);

        // Подтягиваем из базы данные о паспорте,
        // потому что их уже не будет там после удаления (fetchType по дефолту -- LAZY).
        if (lastVersion != null) {
            lastVersion.getPassportValues().forEach(PassportValueEntity::getAttribute);
            lastVersion.setRefBook(refBookEntity);
        }

        dropDataService.drop(refBookVersions.stream()
                .map(RefBookVersionEntity::getStorageCode)
                .collect(Collectors.toSet()));
        refBookRepository.deleteById(refBookId);

        if (lastVersion != null) {
            final RefBookVersionEntity finalVersion = lastVersion;
            auditLogService.addAction(AuditAction.DELETE_REF_BOOK, () -> finalVersion);
        }
    }

    @Override
    @Transactional
    public void toArchive(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);

        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);
        RefBookVersionEntity lastVersion = getLastVersion(refBookEntity.getVersionList());

        // NB: Add checking references to this refBook.
        refBookEntity.setArchived(Boolean.TRUE);

        refBookRepository.save(refBookEntity);

        final RefBookVersionEntity finalVersion = lastVersion;
        auditLogService.addAction(AuditAction.ARCHIVE, () -> finalVersion);
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
    public void changeData(RdmChangeDataRequest request) {

        final String refBookCode = request.getRefBookCode();
        versionValidation.validateRefBookCodeExists(refBookCode);
        RefBookEntity refBook = refBookRepository.findByCode(refBookCode);

        refBookLockService.setRefBookUpdating(refBook.getId());
        try {
            Draft draft = draftService.findDraft(refBookCode);
            if (draft == null) {
                RefBookVersionEntity lastPublishedEntity = versionRepository
                        .findFirstByRefBookIdAndStatusOrderByFromDateDesc(refBook.getId(), RefBookVersionStatus.PUBLISHED);
                draft = draftService.createFromVersion(lastPublishedEntity.getId());
                if (draft == null)
                    throw new UserException(new Message(REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE, refBook.getId()));
            }

            Integer draftId = draft.getId();
            draftService.updateData(new UpdateDataRequest(draftId, draft.getOptLockValue(), request.getRowsToAddOrUpdate()));

            draft = draftService.getDraft(draftId);
            draftService.deleteData(draftId, request.getRowsToDelete(), draft.getOptLockValue());

            draft = draftService.getDraft(draftId);
            publishService.publish(new PublishRequest(draftId, draft.getOptLockValue()));

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
        model.setLastHasConflict(refBookModelData.getLastHasConflict());

        // Use refBookModelData to get RefBookOperation instead of:
        model.setUpdating(entity.isOperation(RefBookOperation.UPDATING));
        model.setPublishing(entity.isOperation(RefBookOperation.PUBLISHING));

        return model;
    }

    /** Проверка на наличие справочников, ссылающихся на указанный справочник. */
    private boolean hasReferrerVersions(String refBookCode) {

        Boolean exists = versionRepository.existsReferrerVersions(refBookCode,
                RefBookStatusType.ALL.name(), RefBookSourceType.ALL.name());
        return Boolean.TRUE.equals(exists);
    }

    private boolean isRefBookRemovable(Integer refBookId) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(isVersionOfRefBook(refBookId));
        where.and(isRemovable().not().or(isArchived()).or(isPublished()));
        return (where.getValue() != null) && !versionRepository.exists(where.getValue());
    }

    private void updateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, String> newPassport) {

        if (newPassport == null || versionEntity == null)
            return;

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

        newPassportValues.addAll(stringPassportToValues(correctUpdatePassport, true, versionEntity));

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

    /** Получение последней (по идентификатору) версии из списка версий. */
    private RefBookVersionEntity getLastVersion(List<RefBookVersionEntity> versions) {

        RefBookVersionEntity result = null;
        for (RefBookVersionEntity version : versions) {
            if (result == null || result.getId() < version.getId())
                result = version;
        }

        return result;
    }

    /** Принудительное обновление значения оптимистической блокировки версии. */
    private void forceUpdateVersionOptLockValue(RefBookVersionEntity versionEntity) {
        try {
            versionEntity.refreshLastActionDate();
            versionRepository.save(versionEntity);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new UserException(OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE, e);
        }
    }
}
