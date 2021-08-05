package ru.i_novus.ms.rdm.impl.async;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationHandler;
import ru.i_novus.ms.rdm.api.async.AsyncOperationStatusEnum;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.i_novus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
class AsyncOperationQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationQueueListener.class);

    private static final String OPERATION_LOG_FORMAT = "id: %s, type: %s";

    private static final String LOG_OPERATION_QUEUE_DESCRIPTION = "Internal async operation queue is '{}'";
    private static final String LOG_OPERATION_FROM_QUEUE_IS_RECEIVED = "Message for operation ({}) from queue is received";
    private static final String LOG_OPERATION_HANDLING_ERROR = "Error while handling deferred operation (%s)";
    private static final String LOG_OPERATION_COMPLETED_WITH_STATUS = "Async operation ({}) is completed with status {}";
    private static final String LOG_OPERATION_FORCING_SAVE = "Async operation ({}) is not yet committed, forcing save.";

    private final AsyncOperationLogEntryRepository repository;

    private final AsyncOperationHandler handler;

    private final Messages messages;

    private final String queueId;

    public AsyncOperationQueueListener(AsyncOperationLogEntryRepository repository,
                                       AsyncOperationHandler handler,
                                       Messages messages,
                                       @Value("${rdm.asyncOperation.queue}")
                                       String queueId) {
        this.repository = repository;
        this.handler = handler;
        this.messages = messages;

        this.queueId = queueId;
    }

    @JmsListener(destination = "${rdm.asyncOperation.queue}", containerFactory = "internalAsyncOperationContainerFactory")
    public void onMessage(AsyncOperationMessage message) {

        if (logger.isInfoEnabled()) {
            logger.info(LOG_OPERATION_QUEUE_DESCRIPTION, queueId);
            logger.info(LOG_OPERATION_FROM_QUEUE_IS_RECEIVED, toOperationLogText(message));
        }

        AsyncOperationLogEntryEntity logEntity = repository.findByUuid(message.getOperationId());
        if (logEntity == null) {
            logEntity = forceSave(message);
        }

        setSecurityContext(message.getUserName());
        logEntity.setStatus(AsyncOperationStatusEnum.IN_PROGRESS);
        logEntity = repository.save(logEntity);

        try {
            logEntity.setSerializableResult(handle(message));
            logEntity.setStatus(AsyncOperationStatusEnum.DONE);

        } catch (Exception e) {

            if (logger.isErrorEnabled()) {
                logger.error(String.format(LOG_OPERATION_HANDLING_ERROR, toOperationLogText(message)), e);
            }

            setErrorContext(e, logEntity);
            logEntity.setStatus(AsyncOperationStatusEnum.ERROR);
        }
        repository.save(logEntity);

        if (logger.isInfoEnabled()) {
            logger.info(LOG_OPERATION_COMPLETED_WITH_STATUS, toOperationLogText(message), logEntity.getStatus());
        }
    }

    private Serializable handle(AsyncOperationMessage message) {

        return handler.handle(message.getOperationType(), message.getCode(), message.getArgs());
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

        if (logger.isWarnEnabled()) {
            logger.warn(LOG_OPERATION_FORCING_SAVE, toOperationLogText(message));
        }

        UUID operationId = message.getOperationId();
        repository.saveWithoutConflict(operationId, message.getOperationType().name(),
                message.getCode(), message.getPayloadAsJson());

        return repository.findByUuid(operationId);
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

    private String toOperationLogText(AsyncOperationMessage message) {

        return String.format(OPERATION_LOG_FORMAT, message.getOperationId(), message.getOperationType());
    }
}
