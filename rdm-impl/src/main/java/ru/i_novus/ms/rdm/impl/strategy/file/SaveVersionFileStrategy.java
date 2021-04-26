package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface SaveVersionFileStrategy extends Strategy {

    void save(RefBookVersion version, FileType fileType, String filePath);
}
