package ru.inovus.ms.rdm.file.export;

import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by znurgaliev on 08.08.2018.
 */
public class PerRowFileGeneratorFactory {

    private PerRowFileGeneratorFactory() {
    }

    public static PerRowFileGenerator getFileGenerator(Iterator<Row> rowIterator, Structure structure, FileType fileType) {

        if (FileType.XLSX.equals(fileType))
            return new XlsFileGenerator(rowIterator, structure);

        throw new RdmException("no generator for " + fileType + " type");
    }

    public static List<FileType> getAvalibleTypes() {
        return Collections.singletonList(FileType.XLSX);
    }
}

