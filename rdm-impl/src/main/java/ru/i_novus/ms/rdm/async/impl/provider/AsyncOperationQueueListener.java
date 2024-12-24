package ru.i_novus.ms.rdm.async.impl.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.service.AsyncOperationMessageService;

import static ru.i_novus.ms.rdm.async.api.util.AsyncOperationUtils.toOperationLogText;

/**
 * Асинхронная операция: Получение и обработка сообщения из очереди.
 */
@Component
@ConditionalOnProperty(name = "rdm.enable.async.operation", havingValue = "true", matchIfMissing = true)
public class AsyncOperationQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueueListener.class);

    private static final String LOG_OPERATION_QUEUE = "Async operation queue is '{}'";
    private static final String LOG_OPERATION_MESSAGE = "Async operation message from queue:\n{}";
    private static final String LOG_OPERATION_COMPLETED_WITH_STATUS = "Async operation completed with status {}:\n{}";

    private AsyncOperationMessageService service;

    @Value("${rdm.async.operation.queue:RDM-INTERNAL-ASYNC-OPERATION-QUEUE}")
    private String queueId;

    @Autowired
    public void setService(@Lazy AsyncOperationMessageService service) {
        this.service = service;
    }

    @JmsListener(destination = "${rdm.async.operation.queue:RDM-INTERNAL-ASYNC-OPERATION-QUEUE}",
            containerFactory = "internalAsyncOperationContainerFactory")
    public void onMessage(AsyncOperationMessage message) {

        if (logger.isInfoEnabled()) {
            logger.info(LOG_OPERATION_QUEUE, queueId);
            logger.info(LOG_OPERATION_MESSAGE, toOperationLogText(message));
        }

        final AsyncOperationLogEntry model = service.receive(message);

        if (logger.isInfoEnabled()) {
            logger.info(LOG_OPERATION_COMPLETED_WITH_STATUS, model.getStatus(), toOperationLogText(message));
        }
    }
}
