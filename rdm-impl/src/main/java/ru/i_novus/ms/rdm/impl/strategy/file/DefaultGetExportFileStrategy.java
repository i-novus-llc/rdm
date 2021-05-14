package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class DefaultGetExportFileStrategy extends AbstractGetExportFileStrategy {

    @Autowired
    private DefaultGenerateFileNameStrategy defaultGenerateFileNameStrategy;

    @Override
    protected boolean allowStore(RefBookVersionEntity entity) {

        return entity != null && !entity.isDraft();
    }

    @Override
    protected String generateZipName(RefBookVersion version, FileType fileType) {

        return defaultGenerateFileNameStrategy.generateZipName(version, fileType);
    }
}
