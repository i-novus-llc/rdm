package ru.inovus.ms.rdm.api.service;

import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.model.refdata.Row;

import java.io.InputStream;
import java.util.Iterator;

public interface VersionFileService {

    InputStream generate(RefBookVersion versionModel, FileType fileType, Iterator<Row> rowIterator);

    void save(RefBookVersion version, FileType fileType, InputStream is);
}
