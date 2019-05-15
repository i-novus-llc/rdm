package ru.inovus.ms.rdm.file.process;

import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.file.*;

public class FileProcessorFactory {
    private FileProcessorFactory() {
    }

    public static FilePerRowProcessor createProcessor(String extension, RowsProcessor rowsProcessor,
                                                      RowMapper rowMapper) {
        switch (extension) {
            case "XLSX":
                return new XlsPerRowProcessor(rowMapper, rowsProcessor);
            case "XML":
                return new XmlPerRowProcessor(rowMapper, rowsProcessor);
            default:
                throw new RdmException("invalid file extension");
        }
    }
}
