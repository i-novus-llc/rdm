package ru.inovus.ms.rdm.impl.async;


import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.async.AsyncOperationStatus;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;
import ru.inovus.ms.rdm.impl.util.AsyncOperationLogEntryUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static ru.inovus.ms.rdm.impl.async.AsyncOperationQueue.QUEUE_ID;

@Component
public class AsyncOperationQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueueListener.class);

    private static final Object NO_RESULT = new Object();
    private static final Exception NO_EXCEPTION = new RuntimeException();

    @Autowired
    private PublishService publishService;

    @Autowired
    private AsyncOperationLogEntryRepository asyncOperationLogEntryRepository;

    @Autowired
    private Messages messages;

    @JmsListener(destination = QUEUE_ID, containerFactory = "internalAsyncOperationContainerFactory")
    public void onOperationReceived(AsyncOperationMessage message) {
        AsyncOperation op = message.getOperation();
        UUID uuid = message.getOperationId();
        Object[] args = message.getArgs();
        String user = message.getUserName();
        logger.info("Message from internal async operation queue received. Operation id: {}", uuid);
        AsyncOperationLogEntryEntity entity = asyncOperationLogEntryRepository.findByUuid(uuid);
        if (entity == null) {
            logger.warn("The entity does not yet committed. Forcing save.");
            asyncOperationLogEntryRepository.saveConflictFree(uuid, op.name(), message.getPayloadAsJson());
            entity = asyncOperationLogEntryRepository.findByUuid(uuid);
        }
        SecurityContextHolder.getContext().setAuthentication(new AbstractAuthenticationToken(emptyList()) {
            @Override public Object getCredentials() {return null;}
            @Override public Object getPrincipal() {return user;}
        });
        entity.setStatus(AsyncOperationStatus.IN_PROGRESS);
        entity = asyncOperationLogEntryRepository.save(entity);
        Pair<Object, Exception> pair = null;
        if (op == AsyncOperation.PUBLICATION) {
            Integer draftId = (Integer) args[0];
            String version = (String) args[1];
            LocalDateTime from = (LocalDateTime) args[2];
            LocalDateTime to = (LocalDateTime) args[3];
            boolean resolveConflicts = (boolean) args[4];
            pair = exec(() -> {
                publishService.publish(draftId, version, from, to, resolveConflicts);
                return NO_RESULT;
            });
        }
        assert pair != null;
        if (pair.getSecond() != NO_EXCEPTION) {
            String error = getErrorMsg(pair);
            entity.setError(error);
            entity.setStatus(AsyncOperationStatus.ERROR);
        } else {
            if (pair.getFirst() != NO_RESULT)
                AsyncOperationLogEntryUtils.setResult(pair.getFirst(), entity);
            entity.setStatus(AsyncOperationStatus.DONE);
        }
        asyncOperationLogEntryRepository.save(entity);
        logger.info("Async operation with id {} completed with status {}", entity.getUuid(), entity.getStatus());
    }

    private String getErrorMsg(Pair<Object, Exception> pair) {
        if (pair.getSecond() instanceof UserException) {
            UserException ue = (UserException) pair.getSecond();
            if (ue.getMessage() != null)
                return messages.getMessage(ue.getMessage(), ue.getArgs());
            else if (ue.getMessages() != null && !ue.getMessages().isEmpty()) {
                return ue.getMessages().stream().map(message -> messages.getMessage(message)).collect(Collectors.joining("\n"));
            } else
                return ue.toString();
        } else
            return pair.getSecond().toString();
    }

    private Pair<Object, Exception> exec(Supplier<?> exec) {
        try {
            Object result = exec.get();
            return Pair.of(result, NO_EXCEPTION);
        } catch (Exception e) {
            logger.error("Error while performing operation.", e);
            return Pair.of(NO_RESULT, e);
        }
    }

}
