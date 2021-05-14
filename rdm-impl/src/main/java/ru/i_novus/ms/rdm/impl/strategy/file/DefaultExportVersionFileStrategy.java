package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.file.FileStorage;

@Component
public class DefaultExportVersionFileStrategy implements ExportVersionFileStrategy {

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private GenerateFileNameStrategy generateFileNameStrategy;

    @Override
    public ExportFile export(RefBookVersion version, FileType fileType, String filePath) {

        return new ExportFile(
                fileStorage.getContent(filePath),
                generateFileNameStrategy.generateZipName(version, fileType)
        );
    }
}
