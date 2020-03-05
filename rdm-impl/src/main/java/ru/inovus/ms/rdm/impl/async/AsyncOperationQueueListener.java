package ru.inovus.ms.rdm.impl.async;


import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static ru.inovus.ms.rdm.impl.async.AsyncOperationQueue.QUEUE_ID;

@Component
class AsyncOperationQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueueListener.class);

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
            asyncOperationLogEntryRepository.saveConflictFree(uuid, message.getCode(), op.name(), message.getPayloadAsJson());
            entity = asyncOperationLogEntryRepository.findByUuid(uuid);
        }
        SecurityContextHolder.getContext().setAuthentication(new AbstractAuthenticationToken(emptyList()) {
            @Override public Object getCredentials() {return null;}
            @Override public Object getPrincipal() {return user;}
        });
        entity.setStatus(AsyncOperationStatus.IN_PROGRESS);
        entity = asyncOperationLogEntryRepository.save(entity);
        try {
            AsyncOperationLogEntryUtils.setResult(handle(op, args), entity);
            entity.setStatus(AsyncOperationStatus.DONE);
        } catch (Exception ex) {
            logger.error("Error while handling deferred operation. Operation type: {}, Operation id: {}", op, uuid, ex);
            String error = getErrorMsg(ex);
            entity.setError(error);
            entity.setStatus(AsyncOperationStatus.ERROR);
        }
        asyncOperationLogEntryRepository.save(entity);
        logger.info("Async operation with id {} completed with status {}", entity.getUuid(), entity.getStatus());
    }

    private Object handle(AsyncOperation op, Object[] args) {
        if (op == AsyncOperation.PUBLICATION) {
            Integer draftId = (Integer) args[0];
            String version = (String) args[1];
            LocalDateTime from = (LocalDateTime) args[2];
            LocalDateTime to = (LocalDateTime) args[3];
            boolean resolveConflicts = (boolean) args[4];
            publishService.publish(draftId, version, from, to, resolveConflicts);
        } else
            throw new IllegalArgumentException("Unrealized operation: " + op);
        return null;
    }

    private String getErrorMsg(Exception ex) {
        if (ex instanceof UserException) {
            UserException ue = (UserException) ex;
            if (ue.getMessage() != null)
                return messages.getMessage(ue.getMessage(), ue.getArgs());
            else if (ue.getMessages() != null && !ue.getMessages().isEmpty()) {
                return ue.getMessages().stream().map(message -> messages.getMessage(message)).collect(Collectors.joining("\n"));
            }
        }
        return ex.toString();
    }

}
