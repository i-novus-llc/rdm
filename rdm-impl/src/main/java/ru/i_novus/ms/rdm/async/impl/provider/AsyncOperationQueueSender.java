package ru.i_novus.ms.rdm.async.impl.provider;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationSender;

import java.util.UUID;

/**
 * Асинхронная операция: Отправка в очередь.
 */
@Component
@ConditionalOnProperty(name = "rdm.enable.async.operation", havingValue = "true", matchIfMissing = true)
public class AsyncOperationQueueSender implements AsyncOperationSender {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueueSender.class);

    private JmsTemplate jmsTemplate;

    @Value("${rdm.async.operation.queue:RDM-INTERNAL-ASYNC-OPERATION-QUEUE}")
    private String queueId;

    @Autowired
    public void setJmsTemplate(@Lazy @Qualifier("queueJmsTemplate") JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public String getName() {
        return "QueueAsyncOperation";
    }

    @Override
    public boolean isSatisfied(AsyncOperationTypeEnum operationType) {
        return true;
    }

    @Override
    public UUID send(AsyncOperationMessage message) {

        logger.info("Sending message to internal async operation queue '{}'. Message:\n{}", queueId, message);
        try {
            jmsTemplate.convertAndSend(queueId, message);

        } catch (Exception e) {
            logger.error("Error while sending message to internal async operation queue.", e);
            throw new UserException("async.operation.queue.not.available");
        }

        return message.getOperationId();
    }
}
