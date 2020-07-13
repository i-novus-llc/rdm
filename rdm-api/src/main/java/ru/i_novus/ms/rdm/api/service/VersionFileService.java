package ru.i_novus.ms.rdm.api.service;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.refdata.Row;

import java.io.InputStream;
import java.util.Iterator;

public interface VersionFileService {

    InputStream generate(RefBookVersion versionModel, FileType fileType, Iterator<Row> rowIterator);

    void save(RefBookVersion version, FileType fileType, InputStream is);
}
