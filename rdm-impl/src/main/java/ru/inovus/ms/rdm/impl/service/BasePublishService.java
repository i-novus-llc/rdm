package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;
import ru.inovus.ms.rdm.api.model.draft.PublishResponse;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.service.*;
import ru.inovus.ms.rdm.api.util.TimeUtils;
import ru.inovus.ms.rdm.api.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.inovus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.*;

@Primary
@Service
class BasePublishService {

    private static final String INVALID_VERSION_NAME_EXCEPTION_CODE = "invalid.version.name";
    private static final String INVALID_VERSION_PERIOD_EXCEPTION_CODE = "invalid.version.period";
    private static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";
    private static final String PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.structure.not.found";
    private static final String PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.data.not.found";

    private RefBookVersionRepository versionRepository;

    private DraftDataService draftDataService;
    private SearchDataService searchDataService;
    private DropDataService dropDataService;

    private RefBookLockService refBookLockService;
    private VersionService versionService;
    private ConflictService conflictService;

    private VersionFileService versionFileService;
    private VersionNumberStrategy versionNumberStrategy;

    private VersionValidation versionValidation;
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    private AuditLogService auditLogService;

    private JmsTemplate jmsTemplate;

    @Value("${rdm.publish.topic:publish_topic}")
    private String publishTopic;

    @Value("${rdm.enable.publish.topic:false}")
    private boolean enablePublishTopic;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public BasePublishService(RefBookVersionRepository versionRepository,
                              DraftDataService draftDataService, SearchDataService searchDataService, DropDataService dropDataService,
                              RefBookLockService refBookLockService, VersionService versionService, ConflictService conflictService,
                              VersionFileService versionFileService, VersionNumberStrategy versionNumberStrategy,
                              VersionValidation versionValidation, VersionPeriodPublishValidation versionPeriodPublishValidation,
                              AuditLogService auditLogService, @Qualifier("topicJmsTemplate") @Autowired(required = false) JmsTemplate jmsTemplate) {
        this.versionRepository = versionRepository;

        this.draftDataService = draftDataService;
        this.searchDataService = searchDataService;
        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;
        this.versionService = versionService;
        this.conflictService = conflictService;

        this.versionFileService = versionFileService;
        this.versionNumberStrategy = versionNumberStrategy;

        this.versionValidation = versionValidation;
        this.versionPeriodPublishValidation = versionPeriodPublishValidation;

        this.auditLogService = auditLogService;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Публикация черновика справочника.
     *
     * @param request параметры публикации
     * @return результат публикации
     */
    @Transactional
    public PublishResponse publish(PublishRequest request) {

        PublishResponse result = new PublishResponse();

        Integer draftId = request.getDraftId();
        RefBookVersionEntity draftEntity = getVersionOrElseThrow(draftId);
        if (draftEntity.getStatus() == RefBookVersionStatus.PUBLISHED)
            return null;

        validatePublishingDraft(draftEntity);

        Integer refBookId = draftEntity.getRefBook().getId();
        String newStorageCode = null;

        refBookLockService.setRefBookPublishing(refBookId);
        try {
            String versionName = getNextVersionNumberOrElseThrow(request.getVersionName(), refBookId);

            LocalDateTime fromDate = request.getFromDate();
            if (fromDate == null) fromDate = TimeUtils.now();
            LocalDateTime toDate = request.getToDate();
            if (toDate != null && fromDate.isAfter(toDate))
                throw new UserException(INVALID_VERSION_PERIOD_EXCEPTION_CODE);

            versionPeriodPublishValidation.validate(fromDate, toDate, refBookId);

            RefBookVersionEntity lastPublishedEntity = getLastPublishedVersionEntity(draftEntity);
            String lastStorageCode = lastPublishedEntity != null ? lastPublishedEntity.getStorageCode() : null;
            newStorageCode = draftDataService.applyDraft(lastStorageCode, draftEntity.getStorageCode(), fromDate, toDate);

            Set<String> droppedDataStorages = new HashSet<>();
            droppedDataStorages.add(draftEntity.getStorageCode());

            draftEntity.setStorageCode(newStorageCode);
            draftEntity.setVersion(versionName);
            draftEntity.setStatus(RefBookVersionStatus.PUBLISHED);
            draftEntity.setFromDate(fromDate);
            draftEntity.setToDate(toDate);

            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId, draftEntity.getId());

            versionRepository.save(draftEntity);

            result.setRefBookCode(draftEntity.getRefBook().getCode());
            result.setOldId(lastPublishedEntity != null ? lastPublishedEntity.getId() : null);
            result.setNewId(draftId);

            // Конфликты могут быть только при наличии
            // ссылочных атрибутов со значениями для ранее опубликованной версии.
            if (result.getOldId() != null) {
                conflictService.discoverConflicts(result.getOldId(), result.getNewId());
            }

            if (lastPublishedEntity != null && lastStorageCode != null
                    && draftEntity.getStructure().storageEquals(lastPublishedEntity.getStructure())) {
                droppedDataStorages.add(lastStorageCode);

                replaceStorageCode(lastStorageCode, newStorageCode);
            }
            dropDataService.drop(droppedDataStorages);

            saveVersionToFiles(draftId);

        } catch (Exception e) {
            if (!StringUtils.isEmpty(newStorageCode)) {
                dropDataService.drop(newStorageCode);
            }

            throw e;

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        auditLogService.addAction(AuditAction.PUBLICATION, () -> draftEntity);
        if (enablePublishTopic)
            jmsTemplate.convertAndSend(publishTopic, draftEntity.getRefBook().getCode());

        return result;
    }

    private RefBookVersionEntity getVersionOrElseThrow(Integer versionId) {

        Optional<RefBookVersionEntity> draftEntityOptional = versionRepository.findById(versionId);
        return draftEntityOptional.orElseThrow(() -> new UserException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, versionId)));
    }

    /** Проверка черновика на возможность публикации. */
    private void validatePublishingDraft(RefBookVersionEntity draftEntity) {

        versionValidation.validateDraftNotArchived(draftEntity.getId());

        if (draftEntity.getStructure() == null || draftEntity.getStructure().isEmpty())
            throw new UserException(new Message(PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE, draftEntity.getRefBook().getCode()));

        if (!searchDataService.hasData(draftEntity.getStorageCode()))
            throw new UserException(new Message(PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE, draftEntity.getRefBook().getCode()));
    }

    public String getNextVersionNumberOrElseThrow(String version, Integer refBookId) {

        if (version == null)
            return versionNumberStrategy.next(refBookId);

        if (!versionNumberStrategy.check(version, refBookId))
            throw new UserException(new Message(INVALID_VERSION_NAME_EXCEPTION_CODE, version));

        return version;
    }

    private RefBookVersionEntity getLastPublishedVersionEntity(RefBookVersionEntity draftVersion) {
        return versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(draftVersion.getRefBook().getId(), RefBookVersionStatus.PUBLISHED);
    }

    /** Замена старого кода хранилища на новый в версиях справочника. */
    private void replaceStorageCode(String oldStorageCode, String newStorageCode) {

        versionRepository.findByStorageCode(oldStorageCode).forEach(entity -> {
            entity.setStorageCode(newStorageCode);
            versionRepository.save(entity);
        });
    }

    /** Корректировка времён в версиях справочника с перекрывающимся периодом времени. */
    private void resolveOverlappingPeriodsInFuture(LocalDateTime fromDate, LocalDateTime toDate,
                                                   Integer refBookId, Integer draftId) {

        if (toDate == null)
            toDate = MAX_TIMESTAMP;

        Iterable<RefBookVersionEntity> entities = versionRepository.findAll(
                hasOverlappingPeriods(fromDate, toDate)
                        .and(isVersionOfRefBook(refBookId))
                        .and(isPublished())
                        // NB: Exclude error "deleted instance passed to merge".
                        .and(hasVersionId(draftId).not())
        );

        entities.forEach(entity -> {
            if (fromDate.isAfter(entity.getFromDate())) {
                entity.setToDate(fromDate);
                versionRepository.save(entity);
            } else {
                versionRepository.deleteById(entity.getId());
            }
        });
    }

    private void saveVersionToFiles(Integer versionId) {

        RefBookVersion draftVersion = versionService.getById(versionId);

        for (FileType fileType : PerRowFileGeneratorFactory.getAvailableTypes()) {
            VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(draftVersion.getId()));
            versionFileService.save(draftVersion, fileType, versionFileService.generate(draftVersion, fileType, dataIterator));
        }
    }
}