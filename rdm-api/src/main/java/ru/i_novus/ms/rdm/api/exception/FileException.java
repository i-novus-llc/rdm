package ru.i_novus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;

public class FileException extends UserException {

    private static final String ABSENT_FILE_EXTENSION_EXCEPTION = "file.extension.absent";
    private static final String INVALID_FILE_EXTENSION_EXCEPTION = "file.extension.invalid";

    protected FileException(Message message) {
        super(message);
    }

    protected FileException(Message message, Throwable cause) {
        super(message, cause);
    }

    public static FileException newAbsentFileExtensionException(String filename) {
        return new FileException(new Message(ABSENT_FILE_EXTENSION_EXCEPTION, filename));
    }

    public static FileException newInvalidFileExtensionException(String invalidExtension) {
        return new FileException(new Message(INVALID_FILE_EXTENSION_EXCEPTION, invalidExtension));
    }
}
