package ru.i_novus.ms.rdm.api.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class AsyncOperationHandler {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationHandler.class);

    private static final String LOG_OPERATION_HANDLING = "Handle operation: type: {}, code: {}";
    private static final String LOG_OPERATION_TYPE_NOT_RESOLVED = "Operation type '%s' is not implemented";

    private final Collection<AsyncOperationResolver> resolvers;

    public AsyncOperationHandler(Collection<AsyncOperationResolver> resolvers) {

        this.resolvers = !isEmpty(resolvers) ? resolvers : emptyList();
    }

    public Serializable handle(AsyncOperationTypeEnum operationType, String code, Serializable[] args) {

        logger.info(LOG_OPERATION_HANDLING, operationType, code);

        List<AsyncOperationResolver> satisfiedResolvers = resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(operationType))
                .collect(toList());

        if (isEmpty(satisfiedResolvers)) {
            String error = String.format(LOG_OPERATION_TYPE_NOT_RESOLVED, operationType);
            logger.error(error);
            throw new NotFoundException(error);
        }

        if (satisfiedResolvers.size() == 1) {

            return satisfiedResolvers.get(0).resolve(code, args);
        }

        return (Serializable) satisfiedResolvers.stream()
                .map(resolver -> resolver.resolve(code, args))
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
