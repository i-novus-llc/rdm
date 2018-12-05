package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.exception.RdmException;

public class FileProcessorFactory {
    private FileProcessorFactory() {
    }

    public static FilePerRowProcessor createProcessor(String extension, RowsProcessor rowsProcessor,
                                                      RowMapper rowMapper, PassportProcessor passportProcessor) {
        switch (extension) {
            case "XLSX":
                return new XlsPerRowProcessor(rowMapper, rowsProcessor);
            case "XML":
                return new XmlPerRowProcessor(rowMapper, rowsProcessor, passportProcessor);
            default:
                throw new RdmException("invalid file extension");
        }
    }
}
