package ru.i_novus.ms.rdm.impl.strategy.refbook;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface FileVersionStrategy extends Strategy {

    void save(RefBookVersion refBookVersion, FileType fileType, String path);
}
