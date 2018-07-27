package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.exception.NsiException;

public class ProcessorFactory {
    private ProcessorFactory() {
    }

    public static FileProcessor createProcessor(String extension, RowsProcessor rowsProcessor, RowMapper rowMapper) {
        switch (extension) {
            case "XLSX":
                return new XlsPerRowProcessor(rowMapper, rowsProcessor);
            case "XML":
                return null;
            default:
                throw new NsiException("invalid file extension");
        }
    }
}
