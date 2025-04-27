package ru.i_novus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;

public class FileContentException extends FileException {

    private static final Message DEFAULT_EXCEPTION = new Message("file.content.invalid");

    public FileContentException(Throwable cause) {
        super(DEFAULT_EXCEPTION, cause);
    }
}
