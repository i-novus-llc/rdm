package ru.i_novus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;

public class FileProcessingException extends FileException {

    private static final Message DEFAULT_EXCEPTION = new Message("file.processing.failed");

    public FileProcessingException(Throwable cause) {
        super(DEFAULT_EXCEPTION, cause);
    }
}
