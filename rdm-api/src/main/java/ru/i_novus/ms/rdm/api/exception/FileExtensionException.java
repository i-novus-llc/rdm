package ru.i_novus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;

public class FileExtensionException extends UserException {

    private static final String ABSENT_FILE_EXTENSION_EXCEPTION = "file.extension.absent";
    private static final String INVALID_FILE_EXTENSION_EXCEPTION = "file.extension.invalid";

    private FileExtensionException(Message message) {
        super(message);
    }

    public static FileExtensionException newAbsentFileExtensionException(String filename) {
        return new FileExtensionException(new Message(ABSENT_FILE_EXTENSION_EXCEPTION, filename));
    }

    public static FileExtensionException newInvalidFileExtensionException(String invalidExtension) {
        return new FileExtensionException(new Message(INVALID_FILE_EXTENSION_EXCEPTION, invalidExtension));
    }
}
