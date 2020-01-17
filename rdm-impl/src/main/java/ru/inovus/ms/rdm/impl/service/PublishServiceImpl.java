package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.inovus.ms.rdm.api.async.Async;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.service.*;
import ru.inovus.ms.rdm.api.util.TimeUtils;
import ru.inovus.ms.rdm.api.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.inovus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.*;

@Primary
@Service
public class PublishServiceImpl implements PublishService {

    private static final String INVALID_VERSION_NAME_EXCEPTION_CODE = "invalid.version.name";
    private static final String INVALID_VERSION_PERIOD_EXCEPTION_CODE = "invalid.version.period";

    private RefBookVersionRepository versionRepository;

    private DraftDataService draftDataService;
    private DropDataService dropDataService;

    private RefBookLockService refBookLockService;
    private VersionService versionService;
    private ConflictService conflictService;
    private ReferenceService referenceService;

    private VersionFileService versionFileService;
    private VersionNumberStrategy versionNumberStrategy;

    private VersionValidation versionValidation;
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    private AuditLogService auditLogService;

    private JmsTemplate jmsTemplate;

    @Autowired
    private AsyncOperationQueue queue;

    @Value("${rdm.publish.topic:publish_topic}")
    private String publishTopic;

    @Value("${rdm.enable.publish.topic:false}")
    private boolean enablePublishTopic;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public PublishServiceImpl(RefBookVersionRepository versionRepository,
                              DraftDataService draftDataService, DropDataService dropDataService,
                              RefBookLockService refBookLockService, VersionService versionService,
                              ConflictService conflictService, ReferenceService referenceService,
                              VersionFileService versionFileService, VersionNumberStrategy versionNumberStrategy,
                              VersionValidation versionValidation, VersionPeriodPublishValidation versionPeriodPublishValidation,
                              AuditLogService auditLogService, @Qualifier("topicJmsTemplate") @Autowired(required = false) JmsTemplate jmsTemplate) {
        this.versionRepository = versionRepository;

        this.draftDataService = draftDataService;
        this.dropDataService = dropDataService;

        this.refBookLockService = refBookLockService;
        this.versionService = versionService;
        this.conflictService = conflictService;
        this.referenceService = referenceService;

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
     * @param draftId          идентификатор черновика справочника
     * @param versionName      версия, под которой публикуется черновик
     *                         (если не указано, используется встроенная нумерация)
     * @param fromDate         дата начала действия опубликованной версии
     * @param toDate           дата окончания действия опубликованной версии
     * @param resolveConflicts признак разрешения конфликтов
     */
    @Override
    @Transactional
    // NB: Use PublishCriteria, required for publishNonConflictReferrers.
    public void publish(Integer draftId, String versionName,
                        LocalDateTime fromDate, LocalDateTime toDate,
                        boolean resolveConflicts) {

        RefBookVersionEntity draftEntity = getVersionOrElseThrow(draftId);
        if (draftEntity.getStatus() == RefBookVersionStatus.PUBLISHED)
            return;
        versionValidation.validateDraftNotArchived(draftId);
        if (draftEntity.getStructure().getAttributes().isEmpty())
            throw new UserException("draft.structure.is-empty");
        validateNotEmpty(draftEntity);
        Integer refBookId = draftEntity.getRefBook().getId();

        refBookLockService.setRefBookPublishing(refBookId);
        try {
            if (versionName == null) {
                versionName = versionNumberStrategy.next(refBookId);

            } else if (!versionNumberStrategy.check(versionName, refBookId)) {
                throw new UserException(new Message(INVALID_VERSION_NAME_EXCEPTION_CODE, versionName));
            }

            if (fromDate == null) fromDate = TimeUtils.now();
            if (toDate != null && fromDate.isAfter(toDate))
                throw new UserException(INVALID_VERSION_PERIOD_EXCEPTION_CODE);

            versionPeriodPublishValidation.validate(fromDate, toDate, refBookId);

            RefBookVersionEntity lastPublishedEntity = getLastPublishedVersionEntity(draftEntity);
            String lastStorageCode = lastPublishedEntity != null ? lastPublishedEntity.getStorageCode() : null;
            String newStorageCode = draftDataService.applyDraft(lastStorageCode, draftEntity.getStorageCode(), fromDate, toDate);

            Set<String> droppedDataStorages = new HashSet<>();
            droppedDataStorages.add(draftEntity.getStorageCode());

            draftEntity.setStorageCode(newStorageCode);
            draftEntity.setVersion(versionName);
            draftEntity.setStatus(RefBookVersionStatus.PUBLISHED);
            draftEntity.setFromDate(fromDate);
            draftEntity.setToDate(toDate);

            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId, draftEntity.getId());

            versionRepository.save(draftEntity);

            if (lastPublishedEntity != null && lastStorageCode != null
                    && draftEntity.getStructure().storageEquals(lastPublishedEntity.getStructure())) {
                droppedDataStorages.add(lastStorageCode);

                versionRepository.findByStorageCode(lastStorageCode).stream()
                        .peek(entity -> entity.setStorageCode(newStorageCode))
                        .forEach(versionRepository::save);
            }
            dropDataService.drop(droppedDataStorages);

            saveDraftToFiles(draftId);

            // NB: Конфликты могут быть только при наличии
            // ссылочных атрибутов со значениями для ранее опубликованной версии.
            if (lastPublishedEntity != null) {
                conflictService.discoverConflicts(lastPublishedEntity.getId(), draftId);
                processDiscoveredConflicts(lastPublishedEntity, draftId, resolveConflicts);
            }

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }
        auditLogService.addAction(
            AuditAction.PUBLICATION,
            () -> draftEntity
        );
        if (enablePublishTopic)
            jmsTemplate.convertAndSend(publishTopic, draftEntity.getRefBook().getCode());
    }

    private void validateNotEmpty(RefBookVersionEntity draftEntity) {
        boolean empty = true;
        for (Structure.Attribute attr : draftEntity.getStructure().getAttributes()) {
            if (draftDataService.isFieldNotEmpty(draftEntity.getStorageCode(), attr.getCode())) {
                empty = false;
                break;
            }
        }
        if (empty)
            throw new UserException("draft.has-no-data");
    }

    @Override
    public UUID publishAsync(Integer draftId, String version, LocalDateTime fromDate, LocalDateTime toDate, boolean resolveConflicts) {
        Map<String, Object> payload = Map.of(Async.PayloadConstants.ARGS_KEY, new Object[] {draftId, version, fromDate, toDate, resolveConflicts});
        return queue.add(UUID.randomUUID(), Async.Operation.PUBLICATION, payload);
    }

    private RefBookVersionEntity getVersionOrElseThrow(Integer versionId) {
        Optional<RefBookVersionEntity> draftEntityOptional = versionRepository.findById(versionId);
        return draftEntityOptional.orElseThrow(() -> new UserException(new Message("draft.not.found", versionId)));
    }

    private RefBookVersionEntity getLastPublishedVersionEntity(RefBookVersionEntity draftVersion) {
        return versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(draftVersion.getRefBook().getCode(), RefBookVersionStatus.PUBLISHED);
    }

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

    private void saveDraftToFiles(Integer draftId) {

        RefBookVersion draftVersion = versionService.getById(draftId);

        for (FileType fileType : PerRowFileGeneratorFactory.getAvailableTypes()) {
            VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(draftVersion.getId()));
            versionFileService.save(draftVersion, fileType,
                    versionFileService.generate(draftVersion, fileType, dataIterator));
        }
    }

    private void processDiscoveredConflicts(RefBookVersionEntity oldVersion, Integer newVersionId, boolean resolveConflicts) {

        // NB: Старые конфликты не удаляются.

        if (resolveConflicts) {
            resolveReferrerConflicts(oldVersion.getRefBook().getCode());
            publishNonConflictReferrers(oldVersion.getRefBook().getCode(), newVersionId);
        }
    }

    /**
     * Обработка разрешаемых конфликтов.
     *
     * @param refBookCode код справочника, на который ссылаются
     */
    private void resolveReferrerConflicts(String refBookCode) {
        referenceService.refreshLastReferrers(refBookCode);
    }

    /**
     * Публикация бесконфликтных справочников, который ссылаются на указанный справочник.
     *
     * @param refBookCode        код справочника, на который ссылаются
     * @param publishedVersionId идентификатор версии справочника
     */
    private void publishNonConflictReferrers(String refBookCode, Integer publishedVersionId) {

        new ReferrerEntityIteratorProvider(versionRepository, refBookCode, RefBookSourceType.DRAFT)
                .iterate().forEachRemaining(referrers ->
            referrers.forEach(referrer -> {
                if (notExistsConflict(referrer.getId(), publishedVersionId))
                    publish(referrer.getId(), null, null, null, false);
            })
        );
    }

    /**
     * Проверка на отсутствие конфликтов версий справочников.
     *
     * @param referrerVersionId  идентификатор версии, которая ссылается
     * @param publishedVersionId идентификатор версии, на которую ссылаются
     * @return Отсутствие конфликта
     */
    private boolean notExistsConflict(Integer referrerVersionId, Integer publishedVersionId) {

        RefBookConflictCriteria criteria = new RefBookConflictCriteria(referrerVersionId, publishedVersionId);
        criteria.setPageSize(1);

        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return conflicts.getContent().isEmpty();
    }
}