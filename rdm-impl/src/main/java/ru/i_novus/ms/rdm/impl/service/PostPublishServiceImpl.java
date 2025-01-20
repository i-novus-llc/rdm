package ru.i_novus.ms.rdm.impl.service;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.impl.provider.PostPublishResolverHandler;
import ru.i_novus.ms.rdm.impl.service.async.AsyncShedLockService;

import java.time.Duration;
import java.time.Instant;

@Service
public class PostPublishServiceImpl implements PostPublishService {

    private static final Logger logger = LoggerFactory.getLogger(PostPublishServiceImpl.class);

    private static final String POST_PUBLISH_JOB_NAME_FORMAT = "post_publish_%s";
    private static final int POST_PUBLISH_MAX_LIVE_HOURS = 1;

    private final PostPublishResolverHandler resolverHandler;

    private final AsyncShedLockService asyncShedLockService;

    @Autowired
    public PostPublishServiceImpl(
            @Lazy PostPublishResolverHandler resolverHandler,
            AsyncShedLockService asyncShedLockService
    ) {
        this.resolverHandler = resolverHandler;
        this.asyncShedLockService = asyncShedLockService;
    }

    public void process(PostPublishRequest request) {

        final String code = request.getRefBookCode();

        try {
            asyncShedLockService.run(
                    getLockConfiguration(code),
                    () -> runPostPublish(request),
                    () -> lockError(code)
            );
        } catch (UserException e) {
            logger.error(e.getMessage());
        }
    }

    private LockConfiguration getLockConfiguration(String code) {

        return new LockConfiguration(
                Instant.now(),
                String.format(POST_PUBLISH_JOB_NAME_FORMAT, code),
                Duration.ofHours(POST_PUBLISH_MAX_LIVE_HOURS),
                Duration.ofSeconds(10)
        );
    }

    private void runPostPublish(PostPublishRequest request) {

        resolverHandler.handle(request);
    }

    private UserException lockError(String code) {

        logger.error("RefBook {} is already post-publishing", code);
        return new UserException(new Message("refbook.with.code.lock.is.post-publishing", code));
    }
}
