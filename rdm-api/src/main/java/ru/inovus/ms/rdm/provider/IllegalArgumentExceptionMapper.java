package ru.inovus.ms.rdm.provider;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.RestExceptionMapper;
import net.n2oapp.platform.jaxrs.RestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentExceptionMapper implements RestExceptionMapper<IllegalArgumentException> {

    private static final Logger logger = LoggerFactory.getLogger(IllegalArgumentExceptionMapper.class);
    private static final String ARGUMENT_ERROR =  "arg.error";

    private Messages messages;

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
