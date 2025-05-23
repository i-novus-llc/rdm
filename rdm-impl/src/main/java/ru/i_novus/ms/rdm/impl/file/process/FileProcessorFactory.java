package ru.i_novus.ms.rdm.impl.file.process;

import ru.i_novus.ms.rdm.api.util.row.RowMapper;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;

import static ru.i_novus.ms.rdm.api.exception.FileException.newInvalidFileExtensionException;

public class FileProcessorFactory {

    private FileProcessorFactory() {
    }

    public static FilePerRowProcessor createProcessor(String extension,
                                                      RowsProcessor rowsProcessor,
                                                      RowMapper rowMapper) {
        switch (extension) {
            case "XLSX": return new XlsxPerRowProcessor(rowMapper, rowsProcessor);
            case "XML": return new XmlPerRowProcessor(rowMapper, rowsProcessor);
            default: throw newInvalidFileExtensionException(extension);
        }
    }
}
