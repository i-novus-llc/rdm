package ru.i_novus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;

public class FileProcessingException extends UserException {

    private static final Message DEFAULT_EXCEPTION = new Message("file.processing.failed");

    public FileProcessingException() {
        super(DEFAULT_EXCEPTION);
    }

    public FileProcessingException(Throwable cause) {
        super(DEFAULT_EXCEPTION, cause);
    }
}
