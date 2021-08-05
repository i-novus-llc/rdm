package ru.i_novus.ms.rdm.impl.async;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    @Qualifier("queueJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    private AsyncOperationLogEntryRepository repository;

    @Autowired
    private UserAccessor userAccessor;

    @Value("${rdm.asyncOperation.queue}")
    private String queueId;

    @Transactional
    public UUID add(AsyncOperationTypeEnum operationType, String code, Serializable[] args) {

        final AsyncOperationMessage message = save(operationType, code, args);
        return message.getOperationId();
    }

    @Transactional
    public UUID send(AsyncOperationTypeEnum operationType, String code, Serializable[] args) {

        final AsyncOperationMessage message = save(operationType, code, args);

        logger.info("Sending message to internal async operation queue '{}'. Message:\n{}", queueId, message);
        try {
            jmsTemplate.convertAndSend(queueId, message);

        } catch (Exception e) {
            logger.error("Error while sending message to internal async operation queue.", e);
            throw new UserException("async.operation.queue.not.available");
        }

        return message.getOperationId();
    }

    private AsyncOperationMessage save(AsyncOperationTypeEnum operationType, String code, Serializable[] args) {

        final UUID operationId = newOperationId();

        AsyncOperationMessage message = new AsyncOperationMessage(operationId, operationType, code, args, userAccessor.get());
        repository.saveWithoutConflict(operationId, operationType.name(), code, message.getPayloadAsJson());

        return message;
    }

    private UUID newOperationId() {
        return UUID.randomUUID();
    }

}
