package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.api.util.VersionNumberStrategy;
import ru.i_novus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates.*;

@Service
class BasePublishService {

    private static final String INVALID_VERSION_NAME_EXCEPTION_CODE = "invalid.version.name";
    private static final String INVALID_VERSION_PERIOD_EXCEPTION_CODE = "invalid.version.period";
    private static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";
    private static final String PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.structure.not.found";
    private static final String PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE = "publishing.draft.data.not.found";

    private final RefBookVersionRepository versionRepository;

    private final DraftDataService draftDataService;
    private final SearchDataService searchDataService;
    private final DropDataService dropDataService;

    private final RefBookLockService refBookLockService;
    private final VersionService versionService;
    private final ConflictService conflictService;

    private final VersionFileService versionFileService;
    private final VersionNumberStrategy versionNumberStrategy;

    private final VersionValidation versionValidation;
    private final VersionPeriodPublishValidation versionPeriodPublishValidation;

    private final AuditLogService auditLogService;

    private final AsyncOperationQueue asyncQueue;

    private final JmsTemplate jmsTemplate;

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
                              AuditLogService auditLogService, AsyncOperationQueue asyncQueue,
                              @Qualifier("topicJmsTemplate") @Autowired(required = false) JmsTemplate jmsTemplate) {
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
        this.asyncQueue = asyncQueue;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Публикация черновика справочника.
     *
     * @param request параметры публикации
     * @return результат публикации
     */
    @Transactional
    public PublishResponse publish(Integer draftId, PublishRequest request) {

        // Получение версии-черновика и Предварительная валидация
        RefBookVersionEntity draftEntity = getVersionOrThrow(draftId);
        if (RefBookVersionStatus.PUBLISHED.equals(draftEntity.getStatus()))
            return null; // Почему не в валидации?

        validatePublishingDraft(draftEntity);

        // Предварительное заполнение значений
        PublishResponse result = new PublishResponse();

        Integer refBookId = draftEntity.getRefBook().getId();
        String oldStorageCode = draftEntity.getStorageCode();
        String newStorageCode = null;

        refBookLockService.setRefBookPublishing(refBookId);
        try {
            versionValidation.validateOptLockValue(draftEntity.getId(), draftEntity.getOptLockValue(), request.getOptLockValue());

            // Дополнительное заполнение значений с валидацией
            // NB: Получение versionName должно быть в одной транзации с сохранением в версии.
            String versionName = nextVersionNumberOrThrow(request.getVersionName(), refBookId);

            LocalDateTime fromDate = request.getFromDate();
            if (fromDate == null) fromDate = TimeUtils.now();

            LocalDateTime toDate = request.getToDate();
            if (toDate != null && fromDate.isAfter(toDate))
                throw new UserException(INVALID_VERSION_PERIOD_EXCEPTION_CODE);

            versionPeriodPublishValidation.validate(fromDate, toDate, refBookId);

            // Получение старой версии
            RefBookVersionEntity lastPublishedEntity = getLastPublishedVersionEntity(draftEntity);

            // Создание и заполнение хранилища новой версии на основе старой версии и версии-черновика
            String lastStorageCode = lastPublishedEntity != null ? lastPublishedEntity.getStorageCode() : null;
            newStorageCode = draftDataService.applyDraft(lastStorageCode, oldStorageCode, fromDate, toDate);

            // Смена версии-черновика на опубликованную версию
            draftEntity.setStorageCode(newStorageCode);
            draftEntity.setVersion(versionName);
            draftEntity.setStatus(RefBookVersionStatus.PUBLISHED);
            draftEntity.setFromDate(fromDate);
            draftEntity.setToDate(toDate);

            resolveOverlappingPeriodsInFuture(fromDate, toDate, refBookId, draftEntity.getId());

            draftEntity.refreshLastActionDate();
            versionRepository.save(draftEntity);

            // Заполнение результата публикации
            result.setRefBookCode(draftEntity.getRefBook().getCode());
            result.setOldId(lastPublishedEntity != null ? lastPublishedEntity.getId() : null);
            result.setNewId(draftId);

            // Обнаружение конфликтов
            // NB: Обнаружение должно быть до удаления хранилища oldStorageCode.

            // Конфликты могут быть только при наличии
            // ссылочных атрибутов со значениями для ранее опубликованной версии.
            if (result.getOldId() != null) {
                conflictService.discoverConflicts(result.getOldId(), result.getNewId());
            }

            // Удаление ненужных хранилищ
            Set<String> droppedDataStorages = new HashSet<>();
            droppedDataStorages.add(oldStorageCode);

            if (lastPublishedEntity != null && lastStorageCode != null
                    && draftEntity.getStructure().storageEquals(lastPublishedEntity.getStructure())) {
                droppedDataStorages.add(lastStorageCode);

                replaceStorageCode(lastStorageCode, newStorageCode);
            }
            dropDataService.drop(droppedDataStorages);

            // Генерация файлов для опубликованной версии
            saveVersionToFiles(draftId);

            // Выполнение действий после публикации
            PostPublishRequest postRequest = new PostPublishRequest(lastStorageCode, oldStorageCode, newStorageCode, fromDate, toDate);
            postPublish(draftEntity.getRefBook().getCode(), postRequest);

        } catch (Exception e) {
            // Откат создания хранилища
            if (!StringUtils.isEmpty(newStorageCode)) {
                dropDataService.drop(newStorageCode);
            }

            throw e;

        } finally {
            refBookLockService.deleteRefBookOperation(refBookId);
        }

        auditLogService.addAction(AuditAction.PUBLICATION, () -> draftEntity);
        if (enablePublishTopic) {
            jmsTemplate.convertAndSend(publishTopic, draftEntity.getRefBook().getCode());
        }

        return result;
    }

    private RefBookVersionEntity getVersionOrThrow(Integer versionId) {

        Optional<RefBookVersionEntity> draftEntityOptional = versionRepository.findById(versionId);
        return draftEntityOptional.orElseThrow(() -> new UserException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, versionId)));
    }

    /** Проверка черновика на возможность публикации. */
    private void validatePublishingDraft(RefBookVersionEntity draftEntity) {

        versionValidation.validateDraftNotArchived(draftEntity.getId());

        if (draftEntity.hasEmptyStructure())
            throw new UserException(new Message(PUBLISHING_DRAFT_STRUCTURE_NOT_FOUND_EXCEPTION_CODE, draftEntity.getRefBook().getCode()));

        if (!searchDataService.hasData(draftEntity.getStorageCode()))
            throw new UserException(new Message(PUBLISHING_DRAFT_DATA_NOT_FOUND_EXCEPTION_CODE, draftEntity.getRefBook().getCode()));
    }

    public String nextVersionNumberOrThrow(String version, Integer refBookId) {

        if (version == null)
            return versionNumberStrategy.next(refBookId);

        if (!versionNumberStrategy.check(version, refBookId))
            throw new UserException(new Message(INVALID_VERSION_NAME_EXCEPTION_CODE, version));

        return version;
    }

    private RefBookVersionEntity getLastPublishedVersionEntity(RefBookVersionEntity draftVersion) {

        final Integer id = draftVersion.getRefBook().getId();
        return versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(id, RefBookVersionStatus.PUBLISHED);
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

    @SuppressWarnings("UnusedReturnValue")
    private UUID postPublish(String code, PostPublishRequest request) {

        // to-do: Отвязать от l10n, например, сделать POST_PUBLICATION.
        return asyncQueue.send(AsyncOperationTypeEnum.L10N_PUBLICATION, code, new Serializable[]{request});
    }
}