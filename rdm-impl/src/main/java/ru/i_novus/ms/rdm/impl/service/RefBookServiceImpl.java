package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.FileExtensionException;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.refbook.*;
import ru.i_novus.ms.rdm.api.model.refdata.DeleteDataRequest;
import ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookDetailModel;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.process.XmlCreateRefBookFileProcessor;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookDetailModelRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.publish.EditPublishStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.CreateFirstStorageStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.CreateFirstVersionStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.CreateRefBookEntityStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.RefBookCreateValidationStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.util.FileUtil;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE;

@Primary
@Service
public class RefBookServiceImpl implements RefBookService {

    private static final String REFBOOK_IS_NOT_CREATED_EXCEPTION_CODE = "refbook.is.not.created";
    private static final String REFBOOK_IS_NOT_CREATED_FROM_XLSX_EXCEPTION_CODE = "refbook.is.not.created.from.xlsx";
    private static final String REFBOOK_HAS_REFERRERS_EXCEPTION_CODE = "refbook.has.referrers";
    private static final String REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE = "refbook.draft.not.found";
    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private final RefBookRepository refBookRepository;
    private final RefBookVersionRepository versionRepository;
    private final RefBookDetailModelRepository refBookDetailModelRepository;

    private final DropDataService dropDataService;

    private final RefBookLockService refBookLockService;

    private final PassportValueRepository passportValueRepository;
    private final RefBookVersionQueryProvider refBookVersionQueryProvider;

    private final VersionValidation versionValidation;

    private final DraftService draftService;
    private final PublishService publishService;

    private final VersionFileService versionFileService;

    private final AuditLogService auditLogService;

    private final StrategyLocator strategyLocator;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public RefBookServiceImpl(RefBookRepository refBookRepository, RefBookVersionRepository versionRepository,
                              RefBookDetailModelRepository refBookDetailModelRepository,
                              DropDataService dropDataService,
                              RefBookLockService refBookLockService,
                              PassportValueRepository passportValueRepository, RefBookVersionQueryProvider refBookVersionQueryProvider,
                              VersionValidation versionValidation, FileStorage fileStorage,
                              DraftService draftService, PublishService publishService,
                              VersionFileService versionFileService,
                              AuditLogService auditLogService,
                              StrategyLocator strategyLocator) {
        this.refBookRepository = refBookRepository;
        this.versionRepository = versionRepository;

        this.refBookDetailModelRepository = refBookDetailModelRepository;

        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;

        this.passportValueRepository = passportValueRepository;
        this.refBookVersionQueryProvider = refBookVersionQueryProvider;

        this.versionValidation = versionValidation;

        this.draftService = draftService;
        this.publishService = publishService;

        this.versionFileService = versionFileService;
        
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
        return entities.map(entity -> refBookModel(entity, criteria.getExcludeDraft()));
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
        return refBookModel(versionEntity, false);
    }

    @Override
    @Transactional
    public String getCode(Integer refBookId) {

        versionValidation.validateRefBookExists(refBookId);
        return refBookRepository.getOne(refBookId).getCode();
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

        final RefBookTypeEnum refBookType = request.getType();
        getStrategy(refBookType, RefBookCreateValidationStrategy.class)
                .validate(request.getCode());

        RefBookEntity refBookEntity = getStrategy(refBookType, CreateRefBookEntityStrategy.class)
                .create(request);

        String storageCode = getStrategy(refBookType, CreateFirstStorageStrategy.class).create();
        RefBookVersionEntity versionEntity = getStrategy(refBookType, CreateFirstVersionStrategy.class)
                .create(request, refBookEntity, storageCode);

        RefBook refBook = refBookModel(versionEntity, false);

        auditLogService.addAction(AuditAction.CREATE_REF_BOOK, () -> versionEntity);

        return refBook;
    }

    @Override
    @Transactional(timeout = 1200000)
    public Draft create(FileModel fileModel) {

        return switch (FileUtil.getExtension(fileModel.getName())) {
            case "XLSX" -> createByXlsx(fileModel);
            case "XML" -> createByXml(fileModel);
            default -> throw new FileExtensionException();
        };
    }

    @SuppressWarnings("unused")
    private Draft createByXlsx(FileModel fileModel) {
        throw new UserException(REFBOOK_IS_NOT_CREATED_FROM_XLSX_EXCEPTION_CODE);
    }

    private Draft createByXml(FileModel fileModel) {

        RefBook refBook;
        // Передавать не сервис, а Consumer, как в DraftServiceImpl.createFromXlsx
        try (XmlCreateRefBookFileProcessor createRefBookFileProcessor = new XmlCreateRefBookFileProcessor(this)) {
            Supplier<InputStream> fileSupplier = versionFileService.supply(fileModel.getPath());
            refBook = createRefBookFileProcessor.process(fileSupplier);
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
        if (!StringUtils.isEmpty(newCode) && !refBookEntity.getCode().equals(newCode)) {
            versionValidation.validateRefBookCode(newCode);
            versionValidation.validateRefBookCodeNotExists(newCode);

            refBookEntity.setCode(newCode);
        }

        refBookEntity.setCategory(request.getCategory());
        updateVersionFromPassport(versionEntity, request.getPassport());
        versionEntity.setComment(request.getComment());

        getStrategy(versionEntity, EditPublishStrategy.class).publish(versionEntity);

        forceUpdateOptLockValue(versionEntity);

        auditLogService.addAction(AuditAction.EDIT_PASSPORT, () -> versionEntity, Map.of("newPassport", request.getPassport()));

        return refBookModel(versionEntity, false);
    }

    @Override
    @Transactional
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void delete(int refBookId) {

        versionValidation.validateRefBookExists(refBookId);
        refBookLockService.validateRefBookNotBusy(refBookId);

        RefBookEntity refBookEntity = refBookRepository.getOne(refBookId);

        if (versionValidation.hasReferrerVersions(refBookEntity.getCode()))
            throw new UserException(new Message(REFBOOK_HAS_REFERRERS_EXCEPTION_CODE, refBookEntity.getCode()));

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

        final RefBookEntity refBook = refBookRepository.findByCode(refBookCode);
        Integer draftId;

        refBookLockService.setRefBookUpdating(refBook.getId());
        try {
            Draft draft = findOrCreateDraft(refBook);
            draftId = draft.getId();

            draftService.updateData(draftId, new UpdateDataRequest(draft.getOptLockValue(), request.getRowsToAddOrUpdate()));

            draft = draftService.getDraft(draftId);
            draftService.deleteData(draftId, new DeleteDataRequest(draft.getOptLockValue(), request.getRowsToDelete()));

        } finally {
            refBookLockService.deleteRefBookOperation(refBook.getId());
        }

        Draft draft = draftService.getDraft(draftId);
        publishService.publish(draftId, new PublishRequest(draft.getOptLockValue()));
    }

    private Draft findOrCreateDraft(RefBookEntity refBook) {

        final Draft draft = draftService.findDraft(refBook.getCode());
        if (draft != null) return draft;

        final RefBookVersionEntity lastPublishedEntity = versionRepository
                .findFirstByRefBookIdAndStatusOrderByFromDateDesc(refBook.getId(), RefBookVersionStatus.PUBLISHED);
        final Draft createdDraft = draftService.createFromVersion(lastPublishedEntity.getId());
        if (createdDraft == null)
            throw new UserException(new Message(REFBOOK_DRAFT_NOT_FOUND_EXCEPTION_CODE, refBook.getId()));

        return createdDraft;
    }

    private RefBook refBookModel(RefBookVersionEntity entity, boolean excludeDraft) {

        if (entity == null) return null;

        final RefBookDetailModel detailModel = refBookDetailModelRepository.findByVersionId(entity.getId());
        return ModelGenerator.refBookModel(entity, detailModel, excludeDraft);
    }

    private RefBookVersionEntity findVersionOrThrow(Integer id) {

        RefBookVersionEntity entity = (id != null) ? versionRepository.findById(id).orElse(null) : null;
        if (entity == null)
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, id));

        return entity;
    }

    private <T extends Strategy> T getStrategy(RefBookTypeEnum refBookType, Class<T> strategy) {

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

        Map<String, String> toInsert = new HashMap<>(newPassport);

        // Удаление существующих атрибутов со значением null.
        Set<String> attributeCodesToRemove = toInsert.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        List<PassportValueEntity> toRemove = versionEntity.getPassportValues().stream()
                .filter(v -> attributeCodesToRemove.contains(v.getAttribute().getCode()))
                .collect(toList());
        versionEntity.getPassportValues().removeAll(toRemove);
        passportValueRepository.deleteAll(toRemove);
        toInsert.entrySet().removeIf(e -> attributeCodesToRemove.contains(e.getKey()));

        // Обновление существующих атрибутов со значением.
        Set<Map.Entry<String, String>> toUpdate = new HashSet<>(toInsert.size());

        toInsert.entrySet().stream()
                .filter(e -> newPassportValues.stream()
                        .anyMatch(v -> e.getKey().equals(v.getAttribute().getCode())))
                .forEach(e -> {
                    newPassportValues.stream()
                            .filter(v -> e.getKey().equals(v.getAttribute().getCode()))
                            .forEach(v -> v.setValue(e.getValue()));
                    toUpdate.add(e);
                });
        toInsert.entrySet().removeAll(toUpdate);

        // Добавление оставшихся несуществующих атрибутов.
        newPassportValues.addAll(RefBookVersionEntity.toPassportValues(toInsert, true, versionEntity));

        versionEntity.setPassportValues(newPassportValues);
    }

    /** Получение последней (по идентификатору) версии из списка версий. */
    private RefBookVersionEntity getLastVersion(List<RefBookVersionEntity> versions) {

        if (CollectionUtils.isEmpty(versions))
            return null;

        RefBookVersionEntity result = null;
        for (RefBookVersionEntity version : versions) {
            if (result == null || result.getCreationDate().isBefore(version.getCreationDate()))
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
