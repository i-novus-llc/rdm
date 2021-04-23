package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Collections.singletonList;

@Component
public class DefaultFindOrCreateFileStrategy implements FindOrCreateFileStrategy {

    @Autowired
    @Lazy
    private VersionService versionService;

    @Autowired
    private VersionFileService versionFileService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private FileNameGenerator fileNameGenerator;

    @Override
    public String findOrCreate(RefBookVersion version, FileType fileType) {

        String path;
        try (InputStream is = generateVersionFile(version, fileType)) {
            path = fileStorage.saveContent(is, fileNameGenerator.generateZipName(version, fileType));

        } catch (IOException e) {
            throw new RdmException(e);
        }

        if (path == null || !fileStorage.isExistContent(path))
            throw new RdmException("Cannot generate file");

        return path;
    }

    private InputStream generateVersionFile(RefBookVersion version, FileType fileType) {

        VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(version.getId()));
        return versionFileService.generate(version, fileType, dataIterator);
    }
}
