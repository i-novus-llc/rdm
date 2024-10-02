package ru.i_novus.ms.rdm.rest.provider;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.RestExceptionMapper;
import net.n2oapp.platform.jaxrs.RestMessage;
import org.hibernate.StaleStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class StaleStateExceptionMapper implements RestExceptionMapper<StaleStateException> {

    private static final String STALE_STATE_ERROR_EXCEPTION_CODE = "stale.state.error";

    private static final Logger logger = LoggerFactory.getLogger(StaleStateExceptionMapper.class);

    private Messages messages;

    public StaleStateExceptionMapper(Messages messages) {
        this.messages = messages;
    }

    @Override
    public RestMessage toMessage(StaleStateException e) {
        logger.error("stale state error", e);
        return new RestMessage(messages.getMessage(STALE_STATE_ERROR_EXCEPTION_CODE) + "\n" + e.getMessage());
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.CONFLICT;
    }
}
