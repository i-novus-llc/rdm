package ru.i_novus.ms.rdm.api.provider;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.RestExceptionMapper;
import net.n2oapp.platform.jaxrs.RestMessage;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;

import java.util.stream.Collectors;

@Provider
public class NotFoundExceptionMapper implements RestExceptionMapper<NotFoundException> {

    private final Messages messages;

    public NotFoundExceptionMapper(Messages messages) {
        this.messages = messages;
    }

    @Override
    public RestMessage toMessage(NotFoundException exception) {
        if (exception.getMessages() != null) {
            return new RestMessage(exception.getMessages().stream().map(this::toError).toList());
        }
        return new RestMessage(messages.getMessage(exception.getMessage(), exception.getArgs()));
    }

    private RestMessage.Error toError(Message message) {
        return new RestMessage.Error(messages.getMessage(message.getCode(), message.getArgs()));
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.NOT_FOUND;
    }
}
