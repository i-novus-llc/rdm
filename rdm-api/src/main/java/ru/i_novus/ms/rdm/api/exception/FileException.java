package ru.i_novus.ms.rdm.api.exception;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;

public class FileException extends UserException {

    private static final String ABSENT_FILE_NAME_EXCEPTION_CODE = "file.name.absent";
    private static final String ABSENT_FILE_EXTENSION_EXCEPTION_CODE = "file.extension.absent";
    private static final String INVALID_FILE_EXTENSION_EXCEPTION_CODE = "file.extension.invalid";

    public FileException(Message message) {
        super(message);
    }

    public static FileException newAbsentFileNameException() {
        return new FileException(new Message(ABSENT_FILE_NAME_EXCEPTION_CODE));
    }

    public static FileException newAbsentFileExtensionException(String filename) {
        return new FileException(new Message(ABSENT_FILE_EXTENSION_EXCEPTION_CODE, filename));
    }

    public static FileException newInvalidFileExtensionException(String invalidExtension) {
        return new FileException(new Message(INVALID_FILE_EXTENSION_EXCEPTION_CODE, invalidExtension));
    }
}
