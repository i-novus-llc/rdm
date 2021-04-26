package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;

@Component
public class UnversionedSaveVersionFileStrategy extends DefaultSaveVersionFileStrategy {

    @Override
    protected void save(VersionFileEntity fileEntity,
                        RefBookVersion version, FileType fileType, String filePath) {
        // Nothing to do.
    }
}
