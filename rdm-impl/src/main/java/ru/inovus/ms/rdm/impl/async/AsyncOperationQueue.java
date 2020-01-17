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
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;
import ru.inovus.ms.rdm.impl.util.AsyncOperationLogEntryUtils;

import java.util.HashMap;
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
    private AsyncOperationLogEntryRepository asyncOperationLogEntryRepository;

    @Autowired
    private UserAccessor userAccessor;

    public UUID add(UUID uuid, Async.Operation op, Map<String, Object> payload) {
        User user = userAccessor.get();
        payload = new HashMap<>(payload == null ? emptyMap() : payload);
        payload.put(Async.PayloadConstants.USER_KEY, user.getUsername());
        asyncOperationLogEntryRepository.save(AsyncOperationLogEntryUtils.createAsyncOperationLogEntryEntity(uuid, Async.Operation.PUBLICATION, payload));
        logger.info("Sending message to internal async op queue. Operation id: {}; Type of operation: {}; Payload: {}", uuid, op, payload);
        try {
            jmsTemplate.convertAndSend(QUEUE_ID, List.of(op, uuid, payload));
        } catch (Exception e) {
            logger.error("Error while sending message to internal async op queue.", e);
            throw new UserException("async.operation.queue.not.available", e);
        }
        return uuid;
    }

}
