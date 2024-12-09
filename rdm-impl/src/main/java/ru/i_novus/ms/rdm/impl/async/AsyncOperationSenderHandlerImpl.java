package ru.i_novus.ms.rdm.impl.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationMessage;
import ru.i_novus.ms.rdm.api.async.AsyncOperationSender;
import ru.i_novus.ms.rdm.api.async.AsyncOperationSenderHandler;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class AsyncOperationSenderHandlerImpl implements AsyncOperationSenderHandler {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationSenderHandlerImpl.class);

    private static final String LOG_OPERATION_HANDLING = "Handle operation to send: type: {}, code: {}";
    private static final String LOG_OPERATION_HANDLING_ARGUMENTS = "Operation arguments:\n{}";
    private static final String LOG_OPERATION_TYPE_NOT_FOUND = "Operation for type '%s' is not implemented";

    private final Collection<AsyncOperationSender> senders;

    public AsyncOperationSenderHandlerImpl(Collection<AsyncOperationSender> senders) {

        this.senders = !isEmpty(senders) ? senders : emptyList();
    }

    @Override
    public Map<String, UUID> handle(AsyncOperationMessage message) {

        if (message == null)
            return null;

        final AsyncOperationTypeEnum operationType = message.getOperationType();
        logger.info(LOG_OPERATION_HANDLING, operationType, message.getCode());

        if (logger.isInfoEnabled()) {
            final Serializable[] args = message.getArgs();
            logger.info(LOG_OPERATION_HANDLING_ARGUMENTS, args != null ? Arrays.asList(args) : "");
        }

        final List<AsyncOperationSender> foundList = senders.stream()
                .filter(sender -> sender.isSatisfied(operationType))
                .collect(toList());

        if (isEmpty(foundList)) {
            final String error = String.format(LOG_OPERATION_TYPE_NOT_FOUND, operationType);
            logger.error(error);
            return null;
        }

        return foundList.stream()
                .map(sender -> send(sender, message))
                .filter(Objects::nonNull)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<String, UUID> send(
            AsyncOperationSender sender,
            AsyncOperationMessage message
    ) {
        final UUID result = sender.send(message);
        return result != null ? new AbstractMap.SimpleEntry<>(sender.getName(), result) : null;
    }
}
