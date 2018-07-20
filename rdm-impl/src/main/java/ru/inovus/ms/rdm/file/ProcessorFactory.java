package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.exception.NsiException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

public class ProcessorFactory {
    private ProcessorFactory() {
    }

    public static FileProcessor createProcessor(String extension, RowsProcessor rowsProcessor,
                                                Structure structure, RefBookVersionRepository versionRepository) {
        switch (extension) {
            case "XLSX":
                return new XlsPerRowProcessor(new DefaultRowMapper(structure, versionRepository), rowsProcessor);
            case "XML":
                return null;
            default:
                throw new NsiException("invalid file extension");
        }
    }
}
