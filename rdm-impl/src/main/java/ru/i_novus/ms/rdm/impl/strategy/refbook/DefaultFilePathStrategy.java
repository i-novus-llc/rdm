package ru.i_novus.ms.rdm.impl.strategy.refbook;

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
import java.util.Collections;

@Component
public class DefaultFilePathStrategy implements FilePathStrategy {

    @Autowired
    private VersionFileService versionFileService;

    @Autowired
    @Lazy
    private VersionService versionService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private FileNameGenerator fileNameGenerator;

    @Override
    public String getPath(RefBookVersion versionModel, FileType fileType) {
        String path;
        try (InputStream is = generateVersionFile(versionModel, fileType)) {
            path = fileStorage.saveContent(is, fileNameGenerator.generateZipName(versionModel, fileType));

        } catch (IOException e) {
            throw new RdmException(e);
        }

        if (path == null || !fileStorage.isExistContent(path))
            throw new RdmException("cannot generate file");

        return path;
    }

    private InputStream generateVersionFile(RefBookVersion versionModel, FileType fileType) {

        VersionDataIterator dataIterator = new VersionDataIterator(versionService, Collections.singletonList(versionModel.getId()));
        return versionFileService.generate(versionModel, fileType, dataIterator);
    }
}
