package ru.inovus.ms.rdm.util;

import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.model.RefBookVersion;

/**
 * Created by znurgaliev on 06.08.2018.
 */
public interface FileNameGenerator {

    String generateName(RefBookVersion version, FileType fileType);

    String generateZipName(RefBookVersion version, FileType fileTypes);
}
