package ru.inovus.ms.rdm.api.util;

import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;

public interface FileNameGenerator {

    String generateName(RefBookVersion version, FileType fileType);

    String generateZipName(RefBookVersion version, FileType fileTypes);
}
