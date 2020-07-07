package ru.inovus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;

public class FileContentException extends UserException {

    private static final Message DEFAULT_EXCEPTION = new Message("file.content.invalid");

    public FileContentException() {
        super(DEFAULT_EXCEPTION);
    }

    public FileContentException(Throwable cause) {
        super(DEFAULT_EXCEPTION, cause);
    }
}
