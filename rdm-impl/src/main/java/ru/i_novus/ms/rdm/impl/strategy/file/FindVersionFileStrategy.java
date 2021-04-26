package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface FindVersionFileStrategy extends Strategy {

    String find(Integer versionId, FileType fileType);
}
