package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface FindOrCreateFileStrategy extends Strategy {

    String findOrCreate(RefBookVersion version, FileType fileType);
}
