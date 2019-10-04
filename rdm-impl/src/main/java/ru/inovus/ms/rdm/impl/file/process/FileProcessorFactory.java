package ru.inovus.ms.rdm.impl.file.process;

import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.impl.file.RowMapper;

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