package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.ReferenceService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.publish.BasePublishStrategy;
import ru.i_novus.ms.rdm.impl.util.ReferrerEntityIteratorProvider;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Primary
@Service
public class PublishServiceImpl implements PublishService {

    private static final Logger logger = LoggerFactory.getLogger(PublishServiceImpl.class);

    private static final String LOG_ERROR_REFRESHING_CONFLICTING_REFERRERS = "Error refreshing conflicting referrers";
    private static final String LOG_ERROR_PUBLISHING_NONCONFLICT_REFERRERS = "Error publishing nonconflict referrers";

    private static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";

    private final RefBookVersionRepository versionRepository;
    private final RefBookConflictRepository conflictRepository;

    private final ReferenceService referenceService;

    private final VersionValidation versionValidation;

    private final AuditLogService auditLogService;

    private final StrategyLocator strategyLocator;

    private final AsyncOperationQueue asyncQueue;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public PublishServiceImpl(RefBookVersionRepository versionRepository,
                              RefBookConflictRepository conflictRepository,
                              ReferenceService referenceService,
                              VersionValidation versionValidation,
                              AuditLogService auditLogService,
                              StrategyLocator strategyLocator,
                              AsyncOperationQueue asyncQueue) {
        this.versionRepository = versionRepository;
        this.conflictRepository = conflictRepository;

        this.referenceService = referenceService;

        this.versionValidation = versionValidation;

        this.auditLogService = auditLogService;

        this.strategyLocator = strategyLocator;

        this.asyncQueue = asyncQueue;
    }

    /**
     * Публикация справочника.
     *
     * @param request параметры публикации
     */
    @Override
    public PublishResponse publish(Integer draftId, PublishRequest request) {

        versionValidation.validateDraftNotArchived(draftId);

        final RefBookVersionEntity entity = getVersionOrThrow(draftId);
        final PublishResponse response = getStrategy(entity, BasePublishStrategy.class).publish(entity, request);
        if (response != null) {
            resolveConflicts(request, response);
        }

        return response;
    }

    @Override
    @Transactional
    public UUID publishAsync(Integer draftId, PublishRequest request) {

        final String code = versionRepository.getOne(draftId).getRefBook().getCode();
        return asyncQueue.send(AsyncOperationTypeEnum.PUBLICATION, code, new Serializable[]{draftId, request});
    }

    private RefBookVersionEntity getVersionOrThrow(Integer versionId) {

        return versionRepository.findById(versionId)
                .orElseThrow(() -> new UserException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, versionId)));
    }

    private <T extends Strategy> T getStrategy(RefBookVersionEntity entity, Class<T> strategy) {

        RefBookEntity refBookEntity = entity != null ? entity.getRefBook() : null;
        return strategyLocator.getStrategy(refBookEntity != null ? refBookEntity.getType() : null, strategy);
    }

    /**
     * Разрешение конфликтов после публикации справочника.
     *
     * @param request  запрос на публикацию
     * @param response результат публикации
     */
    private void resolveConflicts(PublishRequest request, PublishResponse response) {

        if (!request.getResolveConflicts())
            return;

        if (!refreshConflictingReferrers(response))
            return;

        publishNonConflictReferrers(response);
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
                () -> publishReferrerEntity(versionId),
                LOG_ERROR_PUBLISHING_NONCONFLICT_REFERRERS
        );
    }

    private void publishReferrerEntity(Integer versionId) {

        RefBookVersionEntity entity = versionRepository.findById(versionId).orElse(null);
        if (entity == null)
            return;

        publish(versionId, new PublishRequest(entity.getOptLockValue()));
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