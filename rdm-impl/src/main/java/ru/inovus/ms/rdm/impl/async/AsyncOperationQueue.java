package ru.inovus.ms.rdm.impl.async;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.audit.client.UserAccessor;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.util.UUID;

@Component
public class AsyncOperationQueue {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueue.class);

    static final String QUEUE_ID = "RDM-INTERNAL-ASYNC-OPERATION-QUEUE";

    @Autowired
    @Qualifier("queueJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    private AsyncOperationLogEntryRepository asyncOperationLogEntryRepository;

    @Autowired
    private UserAccessor userAccessor;

    @Transactional
    public UUID add(AsyncOperation operation, String code, Object[] args) {

        final UUID uuid = UUID.randomUUID();
        AsyncOperationMessage message = new AsyncOperationMessage(args, userAccessor.get(), uuid, operation, code);
        asyncOperationLogEntryRepository.saveConflictFree(uuid, code, operation.name(), message.getPayloadAsJson());

        logger.info("Sending message to internal async operation queue. Message: {}", message);
        try {
            jmsTemplate.convertAndSend(QUEUE_ID, message);

        } catch (Exception e) {
            logError("Error while sending message to internal async operation queue.", e);
            throw new UserException("async.operation.queue.not.available", e);
        }

        return uuid;
    }

    private void logError(String message, Exception e) {
        logger.error(message, e);
    }
}
