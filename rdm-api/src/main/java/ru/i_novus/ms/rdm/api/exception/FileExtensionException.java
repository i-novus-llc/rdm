package ru.i_novus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;

public class FileExtensionException extends UserException {

    private static final Message DEFAULT_EXCEPTION = new Message("file.extension.invalid");

    public FileExtensionException() {
        super(DEFAULT_EXCEPTION);
    }

    public FileExtensionException(Throwable cause) {
        super(DEFAULT_EXCEPTION, cause);
    }
}
