package ru.i_novus.ms.rdm.async.impl.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationSender;
import ru.i_novus.ms.rdm.async.api.service.AsyncOperationMessageService;

import java.util.UUID;

import static ru.i_novus.ms.rdm.async.api.util.AsyncOperationUtils.toOperationLogText;

/**
 * Асинхронная операция: Синхронный вызов.
 */
@Component
@ConditionalOnProperty(name = "rdm.enable.async.operation", havingValue = "false", matchIfMissing = false)
public class AsyncOperationSynchroSender implements AsyncOperationSender {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationSynchroSender.class);

    private static final String LOG_OPERATION_MESSAGE_SENDING = "Async operation message:\n{}";
    private static final String LOG_OPERATION_COMPLETED_WITH_STATUS = "Async operation completed with status {}:\n{}";

    private AsyncOperationMessageService service;

    @Autowired
    public void setService(@Lazy AsyncOperationMessageService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "SynchroAsyncOperationOperation";
    }

    @Override
    public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
        return true;
    }

    @Override
    public UUID send(AsyncOperationMessage message) {

        if (logger.isInfoEnabled()) {
            logger.info(LOG_OPERATION_MESSAGE_SENDING, toOperationLogText(message));
        }

        final AsyncOperationLogEntry model = service.receive(message);

        if (logger.isInfoEnabled()) {
            logger.info(LOG_OPERATION_COMPLETED_WITH_STATUS, toOperationLogText(message), model.getStatus());
        }

        return message.getOperationId();
    }
    
}
