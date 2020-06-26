package ru.inovus.ms.rdm.impl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;
import ru.inovus.ms.rdm.api.model.draft.PublishResponse;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.ReferenceService;
import ru.inovus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.inovus.ms.rdm.impl.audit.AuditAction;
import ru.inovus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Primary
@Service
public class PublishServiceImpl implements PublishService {

    private static final Logger logger = LoggerFactory.getLogger(PublishServiceImpl.class);

    private static final String LOG_ERROR_REFRESHING_CONFLICTING_REFERRERS = "Error refreshing conflicting referrers";
    private static final String LOG_ERROR_PUBLISHING_NONCONFLICT_REFERRERS = "Error publishing nonconflict referrers";

    private RefBookVersionRepository versionRepository;
    private RefBookConflictRepository conflictRepository;

    private BasePublishService basePublishService;
    private ReferenceService referenceService;

    private AuditLogService auditLogService;

    @Autowired
    private AsyncOperationQueue queue;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public PublishServiceImpl(RefBookVersionRepository versionRepository,
                              RefBookConflictRepository conflictRepository,
                              BasePublishService basePublishService,
                              ReferenceService referenceService,
                              AuditLogService auditLogService) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;

        this.basePublishService = basePublishService;
        this.referenceService = referenceService;

        this.auditLogService = auditLogService;
    }

    /**
     * Публикация справочника.
     *
     * @param request параметры публикации
     */
    @Override
    public void publish(PublishRequest request) {

        PublishResponse response = basePublishService.publish(request);
        if (response == null)
            return;

        if (request.getResolveConflicts()) {
            if (!refreshConflictingReferrers(response))
                return;

            publishNonConflictReferrers(response);
        }
    }

    @Override
    @Transactional
    public UUID publishAsync(PublishRequest request) {

        String code = versionRepository.getOne(request.getDraftId()).getRefBook().getCode();
        return queue.add(AsyncOperation.PUBLICATION, code, new Object[] { request });
    }

    /**
     * Разрешение конфликтов у связанных справочников.
     *
     * @param response результат публикации справочника, на который ссылаются
     * @return Признак успешности
     */
    private boolean refreshConflictingReferrers(PublishResponse response) {
        return tryRun(
                () -> referenceService.refreshLastReferrers(response.getRefBookCode()),
                LOG_ERROR_REFRESHING_CONFLICTING_REFERRERS
        );
    }

    /**
     * Публикация бесконфликтных связанных справочников.
     *
     * @param response результат публикации справочника, на который ссылаются
     */
    private void publishNonConflictReferrers(PublishResponse response) {

        final String refBookCode = response.getRefBookCode();
        final Integer publishedVersionId = response.getNewId();

        AtomicBoolean isOk = new AtomicBoolean(true);

        new ReferrerEntityIteratorProvider(versionRepository, refBookCode, RefBookSourceType.DRAFT)
                .iterate().forEachRemaining(referrers -> {
            Boolean isOkRemaining = referrers.stream()
                    .filter(referrer ->
                            !conflictRepository.existsByReferrerVersionIdAndPublishedVersionId(
                                    referrer.getId(), publishedVersionId)
                    )
                    .map(referrer -> publishReferrer(referrer.getId()))
                    .reduce(true, (result, value) -> result && value);

            if (!Boolean.TRUE.equals(isOkRemaining)) {
                isOk.compareAndSet(true, false);
            }
        });

        if (isOk.get()) {
            auditLogService.addAction(AuditAction.REFERRER_PUBLICATION,
                    () -> versionRepository.getOne(publishedVersionId)
            );
        }
    }

    /**
     * Публикация связанного справочника.
     *
     * @param versionId идентификатор версии связанного справочника
     * @return Признак успешности
     */
    private boolean publishReferrer(Integer versionId) {
        return tryRun(
                () -> publish(new PublishRequest(versionId, null)),
                LOG_ERROR_PUBLISHING_NONCONFLICT_REFERRERS
        );
    }

    /**
     * Запуск действия на выполнение с перехватом ошибок.
     *
     * @param action     действие
     * @param logMessage сообщение для лога при ошибке
     * @return Признак успешности выполнения действия
     */
    private static boolean tryRun(Runnable action, String logMessage) {
        try {
            action.run();

            return true;

        } catch (Exception e) {
            logger.error(logMessage, e);

            return false;
        }
    }
}