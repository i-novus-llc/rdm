package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedGetExportFileStrategy extends AbstractGetExportFileStrategy {

    @Autowired
    private UnversionedGenerateFileNameStrategy unversionedGenerateFileNameStrategy;

    @Override
    protected boolean allowStore(RefBookVersionEntity entity) {

        return false;
    }

    @Override
    protected String generateZipName(RefBookVersion version, FileType fileType) {

        return unversionedGenerateFileNameStrategy.generateZipName(version, fileType);
    }
}
