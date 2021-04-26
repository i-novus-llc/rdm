package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;

import static java.util.Collections.singletonList;

@Component
public class DefaultExportDraftFileStrategy implements ExportDraftFileStrategy {

    @Autowired
    private VersionFileService versionFileService;

    @Autowired
    private FileNameGenerator fileNameGenerator;

    @Override
    public ExportFile export(RefBookVersion version, FileType fileType, VersionService versionService) {

        VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(version.getId()));

        return new ExportFile(
                versionFileService.generate(version, fileType, dataIterator),
                fileNameGenerator.generateZipName(version, fileType)
        );
    }
}
