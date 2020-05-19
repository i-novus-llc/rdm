package ru.inovus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.UserException;
import ru.inovus.ms.rdm.impl.util.FileUtil;
import ru.inovus.ms.rdm.impl.util.mappers.RowMapper;

public class FileProcessorFactory {

    private FileProcessorFactory() {
    }

    public static FilePerRowProcessor createProcessor(String extension,
                                                      RowsProcessor rowsProcessor,
                                                      RowMapper rowMapper) {
        switch (extension) {
            case "XLSX": return new XlsPerRowProcessor(rowMapper, rowsProcessor);
            case "XML": return new XmlPerRowProcessor(rowMapper, rowsProcessor);
            default: throw new UserException(FileUtil.FILE_EXTENSION_INVALID_EXCEPTION_CODE);
        }
    }
}
