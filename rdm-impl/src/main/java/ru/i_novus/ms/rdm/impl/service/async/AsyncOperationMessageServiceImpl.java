package ru.i_novus.ms.rdm.impl.service.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.async.*;
import ru.i_novus.ms.rdm.api.audit.UserAccessor;
import ru.i_novus.ms.rdm.api.service.async.AsyncOperationMessageService;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.i_novus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.AsyncOperationUtils.toOperationLogText;
import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.toJsonString;

@Service
public class AsyncOperationMessageServiceImpl implements AsyncOperationMessageService {

    private static final String LOG_SEND = "Send message:\n{}";
    private static final String LOG_SEND_RESPONSE = "Send response:\n{}";
    private static final String LOG_SEND_RESPONSE_NULL = "Send response: null";
    private static final String LOG_SEND_ERROR = "Error send async operation message:\n%s";
    private static final String LOG_RESOLVE_ERROR = "Error resolve async operation message:\n%s";
    private static final String LOG_FORCE_SAVE = "Async operation message is not yet committed, forcing save:\n{}";

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperationMessageServiceImpl.class);

    private final AsyncOperationLogEntryRepository repository;

    private final AsyncOperationSenderHandler senderHandler;

    private final AsyncOperationResolverHandler resolverHandler;

    private final ObjectMapper objectMapper;

    private final UserAccessor userAccessor;

    private final Messages messages;

    public AsyncOperationMessageServiceImpl(
            AsyncOperationLogEntryRepository repository,
            AsyncOperationSenderHandler senderHandler,
            AsyncOperationResolverHandler resolverHandler,
            @Qualifier("cxfObjectMapper") ObjectMapper objectMapper,
            UserAccessor userAccessor,
            Messages messages
    ) {
        this.repository = repository;
        this.senderHandler = senderHandler;
        this.resolverHandler = resolverHandler;

        this.objectMapper = objectMapper;
        this.userAccessor = userAccessor;
        this.messages = messages;
    }

    @Override
    public AsyncOperationMessage create(AsyncOperationTypeEnum operationType, String code, Serializable[] args) {

        final UUID id = newOperationId();

        final AsyncOperationMessage message = new AsyncOperationMessage(
                id, operationType, code, args, userAccessor.get()
        );
        saveOrUpdate(operationType, code, id, message);

        return message;
    }

    @Override
    @Transactional
    public UUID send(AsyncOperationTypeEnum operationType, String code, Serializable[] args) {

        final AsyncOperationMessage message = create(operationType, code, args);

        if (logger.isInfoEnabled()) {
            logger.info(LOG_SEND, toOperationLogText(message));
        }
        try {
            final Map<String, UUID> result = senderHandler.handle(message);
            if (result != null) {
                if (logger.isInfoEnabled()) {
                    logger.info(LOG_SEND_RESPONSE, toJsonString(objectMapper, result));
                }

            } else {
                logger.info(LOG_SEND_RESPONSE_NULL);

            }

        } catch (Exception e) {
            logger.error(String.format(LOG_SEND_ERROR, toOperationLogText(message)), e);
            throw new UserException("async.operation.not.available");
        }

        return message.getOperationId();
    }

    @Override
    public AsyncOperationLogEntry receive(AsyncOperationMessage message) {

        final AsyncOperationLogEntryEntity logEntity = repository.findByUuid(message.getOperationId());
        final AsyncOperationLogEntryEntity savedEntity = logEntity != null ? logEntity : forceSave(message);

        setSecurityContext(message.getUserName());
        savedEntity.setStatus(AsyncOperationStatusEnum.IN_PROGRESS);
        final AsyncOperationLogEntryEntity workEntity = repository.save(savedEntity);

        try {
            final Map<String, Serializable> result = resolverHandler.handle(message);
            if (result != null) {
                workEntity.setSerializableResult(toJsonString(objectMapper, result));
            }
            workEntity.setStatus(AsyncOperationStatusEnum.DONE);

        } catch (Exception e) {

            if (logger.isErrorEnabled()) {
                logger.error(String.format(LOG_RESOLVE_ERROR, toOperationLogText(message)), e);
            }

            setErrorContext(e, workEntity);
            workEntity.setStatus(AsyncOperationStatusEnum.ERROR);
        }

        final AsyncOperationLogEntryEntity resultEntity = repository.save(workEntity);

        return ModelGenerator.asyncOperationLogEntryModel(resultEntity);
    }

    private AsyncOperationLogEntryEntity forceSave(AsyncOperationMessage message) {

        if (logger.isWarnEnabled()) {
            logger.warn(LOG_FORCE_SAVE, toOperationLogText(message));
        }

        final UUID operationId = message.getOperationId();
        saveOrUpdate(message.getOperationType(), message.getCode(), operationId, message);

        return repository.findByUuid(operationId);
    }

    private void setSecurityContext(String user) {

        SecurityContextHolder.getContext().setAuthentication(

                new AbstractAuthenticationToken(emptyList()) {

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return user;
                    }
                }
        );
    }

    private void setErrorContext(Exception error, AsyncOperationLogEntryEntity logEntity) {

        logEntity.setError(getErrorMsg(error));
        logEntity.setStackTrace(getStackTrace(error));
    }

    private String getErrorMsg(Exception error) {

        if (error instanceof UserException ue) {
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

    private String getStackTrace(Exception error) {

        final StringWriter stringWriter = new StringWriter();
        error.printStackTrace(new PrintWriter(stringWriter));

        return stringWriter.toString();
    }

    private UUID newOperationId() {
        return UUID.randomUUID();
    }

    private void saveOrUpdate(
            AsyncOperationTypeEnum operationType,
            String refBookCode,
            UUID id,
            AsyncOperationMessage message
    ) {
        repository.saveWithoutConflict(
                id,
                operationType.name(),
                refBookCode,
                message.toPayload(objectMapper)
        );
    }
}
