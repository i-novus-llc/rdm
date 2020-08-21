package ru.i_novus.ms.rdm.impl.async;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.audit.client.UserAccessor;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.io.Serializable;
import java.util.UUID;

/**
 * Асинхронная операция: Очередь.
 */
@Component
public class AsyncOperationQueue {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueue.class);

    static final String QUEUE_ID = "RDM-INTERNAL-ASYNC-OPERATION-QUEUE";

    @Autowired
    @Qualifier("queueJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    private AsyncOperationLogEntryRepository repository;

    @Autowired
    private UserAccessor userAccessor;

    @Transactional
    public UUID add(AsyncOperationTypeEnum operationType, String code, Serializable[] args) {

        final UUID operationId = newOperationId();
        AsyncOperationMessage message = new AsyncOperationMessage(operationId, operationType, code, args, userAccessor.get());
        repository.saveWithoutConflict(operationId, operationType.name(), code, message.getPayloadAsJson());

        logger.info("Sending message to internal async operation queue. Message: {}", message);
        try {
            jmsTemplate.convertAndSend(QUEUE_ID, message);

        } catch (Exception e) {
            logger.error("Error while sending message to internal async operation queue.", e);
            throw new UserException("async.operation.queue.not.available");
        }

        return operationId;
    }

    private UUID newOperationId() {
        return UUID.randomUUID();
    }

}
