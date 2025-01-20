package ru.i_novus.ms.rdm.impl.service;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.async.AsyncShedLockService;

import java.time.Duration;
import java.time.Instant;

@Service
public class AsyncPublishService implements PublishService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPublishService.class);

    private static final String PUBLISH_JOB_NAME_FORMAT = "publish_%s";
    private static final int PUBLISH_MAX_LIVE_HOURS = 1;

    private final RefBookVersionRepository versionRepository;

    private final PublishService syncPublishService;

    private final AsyncShedLockService asyncShedLockService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public AsyncPublishService(
            RefBookVersionRepository versionRepository,
            @Lazy @Qualifier("syncPublishService") PublishService syncPublishService,
            AsyncShedLockService asyncShedLockService
    ) {
        this.versionRepository = versionRepository;
        this.syncPublishService = syncPublishService;

        this.asyncShedLockService = asyncShedLockService;
    }

    @Override
    @Transactional
    public void publish(Integer draftId, PublishRequest request) {

        final RefBookVersionEntity draft = versionRepository.getReferenceById(draftId);
        final String code = draft.getRefBook().getCode();

        asyncShedLockService.run(
                getLockConfiguration(code),
                () -> runPublish(draftId, request),
                () -> lockError(code)
        );
    }

    private LockConfiguration getLockConfiguration(String code) {

        return new LockConfiguration(
                Instant.now(),
                String.format(PUBLISH_JOB_NAME_FORMAT, code),
                Duration.ofHours(PUBLISH_MAX_LIVE_HOURS),
                Duration.ofSeconds(10)
        );
    }

    private void runPublish(Integer draftId, PublishRequest request) {

        syncPublishService.publish(draftId, request);
    }

    private UserException lockError(String code) {

        logger.error("RefBook {} is already publishing", code);
        return new UserException(new Message("refbook.with.code.lock.is.publishing", code));
    }
}