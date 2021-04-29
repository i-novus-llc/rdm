package ru.i_novus.ms.rdm.impl.service;

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
import ru.i_novus.ms.rdm.api.enumeration.*;
import ru.i_novus.ms.rdm.api.exception.FileExtensionException;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.refbook.*;
import ru.i_novus.ms.rdm.api.model.refdata.DeleteDataRequest;
import ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.process.XmlCreateRefBookFileProcessor;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.refbook.*;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.util.FileUtil;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates.*;
import static ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE;

@Primary
@Service
public class RefBookServiceImpl implements RefBookService {

    private static final String REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE = "refbook.is.not.created";
    private static final String REFBOOK_IS_NOT_CREATED_FROM_XLSX_EXCEPTION_CODE = "refbook.is.not.created.from.xlsx";
    private static final String REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE = "refbook.draft.not.found";
    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private final RefBookRepository refBookRepository;
    private final RefBookVersionRepository versionRepository;
    private final RefBookModelDataRepository refBookModelDataRepository;

    private final DropDataService dropDataService;

    private final RefBookLockService refBookLockService;

    private final PassportValueRepository passportValueRepository;
    private final RefBookVersionQueryProvider refBookVersionQueryProvider;

    private final VersionValidation versionValidation;

    private final FileStorage fileStorage;

    private final DraftService draftService;
    private final PublishService publishService;

    private final AuditLogService auditLogService;

    private final StrategyLocator strategyLocator;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public RefBookServiceImpl(RefBookRepository refBookRepository, RefBookVersionRepository versionRepository,
                              RefBookModelDataRepository refBookModelDataRepository,
                              DropDataService dropDataService,
                              RefBookLockService refBookLockService,
                              PassportValueRepository passportValueRepository, RefBookVersionQueryProvider refBookVersionQueryProvider,
                              VersionValidation versionValidation, FileStorage fileStorage,
                              DraftService draftService, PublishService publishService,
                              AuditLogService auditLogService,
                              StrategyLocator strategyLocator) {
        this.refBookRepository = refBookRepository;
        this.versionRepository = versionRepository;

        this.refBookModelDataRepository = refBookModelDataRepository;

        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;

        this.passportValueRepository = passportValueRepository;
        this.refBookVersionQueryProvider = refBookVersionQueryProvider;

        this.versionValidation = versionValidation;
        this.fileStorage = fileStorage;

        this.draftService = draftService;
        this.publishService = publishService;

        this.auditLogService = auditLogService;

        this.strategyLocator = strategyLocator;
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

        RefBookVersionEntity versionEntity = findVersionOrThrow(versionId);
        boolean hasReferrerVersions = hasReferrerVersions(versionEntity.getRefBook().getCode());

        return refBookModel(versionEntity, hasReferrerVersions,
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.DRAFT),
                getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED)
        );
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

        final RefBookType refBookType = request.getType();
        getStrategy(refBookType, RefBookCreateValidationStrategy.class)
                .validate(request.getCode());

        RefBookEntity refBookEntity = getStrategy(refBookType, CreateRefBookEntityStrategy.class)
                .create(request);

        String storageCode = getStrategy(refBookType, CreateFirstStorageStrategy.class).create();
        RefBookVersionEntity versionEntity = getStrategy(refBookType, CreateFirstVersionStrategy.class)
                .create(request, refBookEntity, storageCode);

        RefBook refBook = refBookModel(versionEntity, false,
            getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.DRAFT),
            getSourceTypeVersion(versionEntity.getRefBook().getId(), RefBookSourceType.LAST_PUBLISHED)
        );

        auditLogService.addAction(AuditAction.CREATE_REF_BOOK, () -> versionEntity);

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

        RefBookVersionEntity versionEntity = findVersionOrThrow(request.getVersionId());
        getStrategy(versionEntity, ValidateVersionNotArchivedStrategy.class).validate(versionEntity);

        RefBookEntity refBookEntity = versionEntity.getRefBook();
        refBookLockService.validateRefBookNotBusy(refBookEntity.getId());
        versionValidation.validateOptLockValue(versionEntity.getId(), versionEntity.getOptLockValue(), request.getOptLockValue());

        final String newCode = request.getCode();
        if (!isEmpty(newCode) && !refBookEntity.getCode().equals(newCode)) {
            versionValidation.validateRefBookCode(newCode);
            versionValidation.validateRefBookCodeNotExists(newCode);

            refBookEntity.setCode(newCode);
        }

        refBookEntity.setCategory(request.getCategory());
        updateVersionFromPassport(versionEntity, request.getPassport());
        versionEntity.setComment(request.getComment());

        forceUpdateOptLockValue(versionEntity);

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
        refBookLockService.validateRefBookNotBusy(refBookId);

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
            draftService.updateData(draftId, new UpdateDataRequest(draft.getOptLockValue(), request.getRowsToAddOrUpdate()));

            draft = draftService.getDraft(draftId);
            draftService.deleteData(draftId, new DeleteDataRequest(draft.getOptLockValue(), request.getRowsToDelete()));

            draft = draftService.getDraft(draftId);
            publishService.publish(draftId, new PublishRequest(draft.getOptLockValue()));

        } finally {
            refBookLockService.deleteRefBookOperation(refBook.getId());
        }
    }

    private RefBook refBookModel(RefBookVersionEntity entity,
                                 List<RefBookVersionEntity> draftVersions, List<RefBookVersionEntity> lastPublishVersions) {
        if (entity == null) return null;

        RefBookVersionEntity draftVersion = getRefBookVersion(entity.getRefBook().getId(), draftVersions);
        RefBookVersionEntity lastPublishedVersion = getRefBookVersion(entity.getRefBook().getId(), lastPublishVersions);

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
        List<Structure.Attribute> primaries = (structure != null) ? structure.getPrimaries() : emptyList();
        model.setHasPrimaryAttribute(!primaries.isEmpty());

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

    private RefBookVersionEntity findVersionOrThrow(Integer id) {

        RefBookVersionEntity entity = (id != null) ? versionRepository.findById(id).orElse(null) : null;
        if (entity == null)
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, id));

        return entity;
    }

    private <T extends Strategy> T getStrategy(RefBookType refBookType, Class<T> strategy) {

        return strategyLocator.getStrategy(refBookType, strategy);
    }

    private <T extends Strategy> T getStrategy(RefBookVersionEntity entity, Class<T> strategy) {

        return strategyLocator.getStrategy(entity != null ? entity.getRefBook().getType() : null, strategy);
    }

    private void updateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, String> newPassport) {

        if (newPassport == null || newPassport.isEmpty() || versionEntity == null)
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

        Set<Map.Entry<String, String>> toUpdate = correctUpdatePassport.entrySet().stream()
                .filter(e -> newPassportValues.stream()
                        .anyMatch(v -> e.getKey().equals(v.getAttribute().getCode())))
                .peek(e -> newPassportValues.stream()
                        .filter(v -> e.getKey().equals(v.getAttribute().getCode()))
                        .findAny().get().setValue(e.getValue()))
                .collect(Collectors.toSet());
        correctUpdatePassport.entrySet().removeAll(toUpdate);

        newPassportValues.addAll(RefBookVersionEntity.toPassportValues(correctUpdatePassport, true, versionEntity));

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
        return getRefBookVersion(refBookId, versions);
    }

    private RefBookVersionEntity getRefBookVersion(Integer refBookId, List<RefBookVersionEntity> versions) {
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

    /** Принудительное сохранение для обновления значения оптимистической блокировки версии. */
    private void forceUpdateOptLockValue(RefBookVersionEntity versionEntity) {
        try {
            versionEntity.refreshLastActionDate();
            versionRepository.save(versionEntity);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new UserException(OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE, e);
        }
    }
}
