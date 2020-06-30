package ru.inovus.ms.rdm.api.provider;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.RestExceptionMapper;
import net.n2oapp.platform.jaxrs.RestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.OptimisticLockException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class OptimisticLockExceptionMapper implements RestExceptionMapper<OptimisticLockException> {

    private static final String OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE = "optimistic.lock.error";

    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockExceptionMapper.class);

    private Messages messages;

    public OptimisticLockExceptionMapper(Messages messages) {
        this.messages = messages;
    }

    @Override
    public RestMessage toMessage(OptimisticLockException e) {
        logger.error("optimistic lock error", e);
        return new RestMessage(messages.getMessage(OPTIMISTIC_LOCK_ERROR_EXCEPTION_CODE));
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.CONFLICT;
    }
}
