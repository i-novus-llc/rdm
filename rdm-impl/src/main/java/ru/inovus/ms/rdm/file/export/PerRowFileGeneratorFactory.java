package ru.inovus.ms.rdm.file.export;

import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Row;

import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by znurgaliev on 08.08.2018.
 */
public class PerRowFileGeneratorFactory {

    private PerRowFileGeneratorFactory() {
    }

    public static PerRowFileGenerator getFileGenerator(Iterator<Row> rowIterator, RefBookVersion version, FileType fileType) {

        if (FileType.XLSX.equals(fileType))
            return new XlsFileGenerator(rowIterator, version.getStructure());

        if (FileType.XML.equals(fileType))
            return new XmlFileGenerator(rowIterator, version);

        throw new RdmException("no generator for " + fileType + " type");
    }

    public static List<FileType> getAvailableTypes() {
        return asList(FileType.XLSX, FileType.XML);
    }
}

