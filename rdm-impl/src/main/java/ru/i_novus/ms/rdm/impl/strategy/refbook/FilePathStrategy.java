package ru.i_novus.ms.rdm.impl.strategy.refbook;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface FilePathStrategy extends Strategy {

    String getPath(RefBookVersion refBookVersion, FileType fileType);
}
