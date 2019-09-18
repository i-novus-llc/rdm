package ru.inovus.ms.rdm.n2o.service.api;

import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.n2o.model.version.RefBookVersion;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;

import java.io.InputStream;
import java.util.Iterator;

public interface VersionFileService {

    InputStream generate(RefBookVersion versionModel, FileType fileType, Iterator<Row> rowIterator);

    void save(RefBookVersion version, FileType fileType, InputStream is);
}
