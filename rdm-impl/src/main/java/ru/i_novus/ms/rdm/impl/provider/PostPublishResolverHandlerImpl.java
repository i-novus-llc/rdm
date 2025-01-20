package ru.i_novus.ms.rdm.impl.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class PostPublishResolverHandlerImpl implements PostPublishResolverHandler {

    private static final Logger logger = LoggerFactory.getLogger(PostPublishResolverHandlerImpl.class);

    private static final String LOG_OPERATION_HANDLING_REQUEST = "Handle operation to resolve post-publish request: {}";
    private static final String LOG_OPERATION_RESOLVERS_NOT_FOUND = "Post-publish resolvers not found";

    private final Collection<PostPublishResolver> resolvers;

    public PostPublishResolverHandlerImpl(Collection<PostPublishResolver> resolvers) {

        this.resolvers = !isEmpty(resolvers) ? resolvers : emptyList();
    }

    @Override
    public void handle(PostPublishRequest request) {

        if (logger.isInfoEnabled()) {
            logger.info(LOG_OPERATION_HANDLING_REQUEST, request);
        }

        if (isEmpty(resolvers)) {
            logger.warn(LOG_OPERATION_RESOLVERS_NOT_FOUND);
            return;
        }

        resolvers.forEach(resolver -> resolve(resolver, request));
    }

    private void resolve(PostPublishResolver resolver, PostPublishRequest request) {

        // to-do: Обработка ошибок?!
        resolver.resolve(request);
    }
}
