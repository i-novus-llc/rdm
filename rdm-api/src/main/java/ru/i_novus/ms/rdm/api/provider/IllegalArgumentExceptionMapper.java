package ru.i_novus.ms.rdm.api.provider;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.RestExceptionMapper;
import net.n2oapp.platform.jaxrs.RestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class IllegalArgumentExceptionMapper implements RestExceptionMapper<IllegalArgumentException> {

    private static final Logger logger = LoggerFactory.getLogger(IllegalArgumentExceptionMapper.class);

    private static final String ARGUMENT_ERROR = "arg.error";

    private final Messages messages;

    public IllegalArgumentExceptionMapper(Messages messages) {
        this.messages = messages;
    }

    @Override
    public RestMessage toMessage(IllegalArgumentException e) {
        logger.error("receive error", e);
        return new RestMessage(messages.getMessage(ARGUMENT_ERROR));
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
