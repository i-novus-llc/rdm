package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.UserException;
import ru.inovus.ms.rdm.exception.RdmException;

public class FileProcessorFactory {
    private FileProcessorFactory() {
    }

    public static FilePerRowProcessor createProcessor(String extension, RowsProcessor rowsProcessor, RowMapper rowMapper) {
        switch (extension) {
            case "XLSX":
                return new XlsPerRowProcessor(rowMapper, rowsProcessor);
            case "XML":
                throw new UserException("XML not support");
            default:
                throw new RdmException("invalid file extension");
        }
    }
}
