package ru.i_novus.ms.rdm.async.impl.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationResolver;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationResolverHandler;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class AsyncOperationResolverHandlerImpl implements AsyncOperationResolverHandler {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationResolverHandlerImpl.class);

    private static final String LOG_OPERATION_HANDLING = "Handle operation to resolve: type: {}, code: {}";
    private static final String LOG_OPERATION_HANDLING_ARGUMENTS = "Operation arguments:\n{}";
    private static final String LOG_OPERATION_TYPE_NOT_FOUND = "Operation for type '%s' is not implemented";

    private final Collection<AsyncOperationResolver> resolvers;

    public AsyncOperationResolverHandlerImpl(Collection<AsyncOperationResolver> resolvers) {

        this.resolvers = !isEmpty(resolvers) ? resolvers : emptyList();
    }

    @Override
    public Map<String, Serializable> handle(AsyncOperationMessage message) {

        final AsyncOperationTypeEnum operationType = message.getOperationType();
        logger.info(LOG_OPERATION_HANDLING, operationType, message.getCode());

        if (logger.isInfoEnabled()) {
            final Serializable[] args = message.getArgs();
            logger.info(LOG_OPERATION_HANDLING_ARGUMENTS, args != null ? Arrays.asList(args) : "");
        }

        final List<AsyncOperationResolver> foundList = resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(operationType))
                .collect(toList());

        if (isEmpty(foundList)) {
            final String error = String.format(LOG_OPERATION_TYPE_NOT_FOUND, operationType);
            logger.error(error);
            return null;
        }

        return foundList.stream()
                .map(resolver -> resolve(resolver, message))
                .filter(Objects::nonNull)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<String, Serializable> resolve(
            AsyncOperationResolver resolver,
            AsyncOperationMessage message
    ) {
        final Serializable result = resolver.resolve(message);
        return result != null ? new AbstractMap.SimpleEntry<>(resolver.getName(), result) : null;
    }
}
