package ru.inovus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;

import java.util.List;

public class NotFoundException extends UserException {

    private static final Message NOT_FOUND_EXCEPTION = new Message("resource.not.found");

    public NotFoundException(Message message) {
        super(message);
    }

    public NotFoundException(List<Message> messages) {
        super(messages);
    }

    public NotFoundException(Message message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(String code) {
        super(code);
    }

    public NotFoundException(String code, Throwable cause) {
        super(code, cause);
    }

    public NotFoundException() {
        super(NOT_FOUND_EXCEPTION);
    }

    public NotFoundException(Throwable cause) {
        super(NOT_FOUND_EXCEPTION, cause);
    }

}