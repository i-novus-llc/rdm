package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface GenerateFileNameStrategy extends Strategy {

    String generateName(RefBookVersion version, FileType fileType);

    String generateZipName(RefBookVersion version, FileType fileType);
}
