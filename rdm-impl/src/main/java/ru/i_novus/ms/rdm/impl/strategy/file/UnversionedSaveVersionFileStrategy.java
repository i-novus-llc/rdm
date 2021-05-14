package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

@Component
public class UnversionedSaveVersionFileStrategy implements SaveVersionFileStrategy {

    @Override
    public void save(RefBookVersion version, FileType fileType, String filePath) {
        // Nothing to do.
    }
}
