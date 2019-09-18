package ru.inovus.ms.rdm.n2o.util;

import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.n2o.model.version.RefBookVersion;

public interface FileNameGenerator {

    String generateName(RefBookVersion version, FileType fileType);

    String generateZipName(RefBookVersion version, FileType fileTypes);
}
