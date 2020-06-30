package ru.inovus.ms.rdm.impl.async;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.async.AsyncOperationStatus;
import ru.inovus.ms.rdm.api.model.draft.PublishRequest;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;
import ru.inovus.ms.rdm.impl.util.AsyncOperationLogEntryUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.impl.async.AsyncOperationQueue.QUEUE_ID;

@Component
class AsyncOperationQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueueListener.class);

    private final PublishService publishService;

    private final AsyncOperationLogEntryRepository asyncOperationLogEntryRepository;

    private final Messages messages;

    public AsyncOperationQueueListener(PublishService publishService,
                                       AsyncOperationLogEntryRepository asyncOperationLogEntryRepository,
                                       Messages messages) {
        this.publishService = publishService;
        this.asyncOperationLogEntryRepository = asyncOperationLogEntryRepository;
        this.messages = messages;
    }

    @JmsListener(destination = QUEUE_ID, containerFactory = "internalAsyncOperationContainerFactory")
    public void onMessage(AsyncOperationMessage message) {

        AsyncOperation operation = message.getOperation();
        UUID uuid = message.getOperationId();
        Object[] args = message.getArgs();
        logger.info("Message from internal async operation queue is received. Operation id: {}", uuid);

        AsyncOperationLogEntryEntity logEntity = asyncOperationLogEntryRepository.findByUuid(uuid);
        if (logEntity == null) {
            logEntity = forceSave(message);
        }

        setSecurityContext(message.getUserName());
        logEntity.setStatus(AsyncOperationStatus.IN_PROGRESS);
        logEntity = asyncOperationLogEntryRepository.save(logEntity);

        try {
            AsyncOperationLogEntryUtils.setResult(handle(operation, args), logEntity);
            logEntity.setStatus(AsyncOperationStatus.DONE);

        } catch (Exception e) {
            logger.error("Error while handling deferred operation. Operation type: {}, Operation id: {}", operation, uuid, e);
            setErrorContext(e, logEntity);
            logEntity.setStatus(AsyncOperationStatus.ERROR);
        }
        asyncOperationLogEntryRepository.save(logEntity);

        logger.info("Async operation with id {} is completed with status {}", logEntity.getUuid(), logEntity.getStatus());
    }

    private Object handle(AsyncOperation operation, Object[] args) {

        if (operation == AsyncOperation.PUBLICATION) {
            return handlePublication(args);
        }

        throw new IllegalArgumentException(String.format("Operation '%s' is not implemented", operation.toString()));
    }

    private Object handlePublication(Object[] args) {

        PublishRequest request;

        Object arg = args[0];
        if (arg instanceof PublishRequest) {
            request = (PublishRequest) arg;

        } else {
            request = new PublishRequest((Integer) arg, null);
            request.setVersionName((String) args[1]);
            request.setFromDate((LocalDateTime) args[2]);
            request.setToDate((LocalDateTime) args[3]);
            request.setResolveConflicts((boolean) args[4]);
        }

        publishService.publish(request);

        return null;
    }

    private void setSecurityContext(String user) {

        SecurityContextHolder.getContext()
                .setAuthentication(new AbstractAuthenticationToken(emptyList()) {
                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return user;
                    }
                });
    }

    private AsyncOperationLogEntryEntity forceSave(AsyncOperationMessage message) {

        logger.warn("The log entity is not yet committed. Forcing save.");

        UUID uuid = message.getOperationId();
        asyncOperationLogEntryRepository.saveConflictFree(uuid, message.getCode(),
                message.getOperation().name(), message.getPayloadAsJson());

        return asyncOperationLogEntryRepository.findByUuid(uuid);
    }

    private void setErrorContext(Exception error, AsyncOperationLogEntryEntity logEntity) {

        logEntity.setError(getErrorMsg(error));
        logEntity.setStackTrace(getStackTrace(error));
    }

    private String getStackTrace(Exception error) {

        StringWriter stringWriter = new StringWriter();
        error.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private String getErrorMsg(Exception error) {

        if (error instanceof UserException) {
            UserException ue = (UserException) error;
            if (ue.getMessage() != null)
                return messages.getMessage(ue.getMessage(), ue.getArgs());

            if (!isEmpty(ue.getMessages())) {
                return ue.getMessages().stream()
                        .map(messages::getMessage)
                        .collect(joining("\n"));
            }
        }

        return error.toString();
    }
}
