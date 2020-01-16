package ru.inovus.ms.rdm.impl.async;

import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.audit.client.UserAccessor;
import ru.i_novus.ms.audit.client.model.User;
import ru.inovus.ms.rdm.api.async.Async;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;

@Component
public class AsyncOperationQueue {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueue.class);

    static final String QUEUE_ID = "RDM-INTERNAL-ASYNC-OPERATION-QUEUE";

    static final int OP_IDX = 0;
    static final int OP_ID_IDX = 1;
    static final int OP_PAYLOAD_IDX = 2;
    static final int USER_NAME_IDX = 3;

    @Autowired
    @Qualifier("queueJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    private UserAccessor userAccessor;

    public void add(UUID opId, Async.Operation op, Map<String, Object> payload) {
        payload = payload == null ? emptyMap() : payload;
        User user = userAccessor.get();
        logger.info("Sending message to internal async op queue. Operation id: {}; Type of operation: {}; Payload: {}, User: {}", opId, op, payload, user.getUsername());
        try {
            jmsTemplate.convertAndSend(QUEUE_ID, List.of(op, opId, payload, user.getUsername()));
        } catch (Exception e) {
            logger.error("Error while sending message to internal async op queue.", e);
            throw new UserException("async.operation.queue.not.available", e);
        }
    }

}
