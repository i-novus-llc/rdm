package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Collections.singletonList;

@Component
public class DefaultCreateVersionFileStrategy implements CreateVersionFileStrategy {

    @Autowired
    private VersionFileService versionFileService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private GenerateFileNameStrategy generateFileNameStrategy;

    @Override
    public String create(RefBookVersion version, FileType fileType, VersionService versionService) {

        String filePath;
        try (InputStream is = generateVersionFile(version, fileType, versionService)) {
            filePath = fileStorage.saveContent(is, generateFileNameStrategy.generateZipName(version, fileType));

        } catch (IOException e) {
            throw new RdmException(e);
        }

        if (filePath == null || !fileStorage.isExistContent(filePath))
            throw new RdmException("Cannot generate file");

        return filePath;
    }

    private InputStream generateVersionFile(RefBookVersion version, FileType fileType, VersionService versionService) {

        VersionDataIterator dataIterator = new VersionDataIterator(versionService, singletonList(version.getId()));
        return versionFileService.generate(version, fileType, dataIterator);
    }
}
