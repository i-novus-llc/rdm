package ru.inovus.ms.rdm.impl.file.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.api.exception.FileContentException;
import ru.inovus.ms.rdm.api.exception.FileProcessingException;

public class FileParseUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileParseUtils.class);

    public static final String FILE_CONTENT_INVALID_EXCEPTION_CODE = "file.content.invalid";

    public static final String LOG_FILE_CONTENT_ERROR = "Error with file content.";
    public static final String LOG_FILE_PROCESSING_ERROR = "Error while processing file.";

    private FileParseUtils() {
        throw new UnsupportedOperationException();
    }

    public static void throwFileContentError(Exception e) {
        logger.error(LOG_FILE_CONTENT_ERROR, e);
        throw new FileContentException(e);
    }

    public static void throwFileProcessingError(Exception e) {
        logger.error(LOG_FILE_PROCESSING_ERROR, e);
        throw new FileProcessingException(e);
    }
}
