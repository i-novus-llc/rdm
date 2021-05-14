package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Collections.singletonList;

public abstract class AbstractGetExportFileStrategy implements GetExportFileStrategy {

    @Autowired
    private VersionFileRepository versionFileRepository;

    @Autowired
    private VersionFileService versionFileService;

    @Autowired
    private FileStorage fileStorage;

    @Override
    public ExportFile get(RefBookVersionEntity entity, FileType fileType, VersionService versionService) {

        RefBookVersion version = ModelGenerator.versionModel(entity);

        boolean allowStore = allowStore(entity);
        String filePath = allowStore ? find(version.getId(), fileType) : null;
        if (filePath == null) {
            filePath = create(version, fileType, versionService);

            if (allowStore) {
                save(version, fileType, filePath);
            }
        }

        return export(version, fileType, filePath);
    }

    protected abstract boolean allowStore(RefBookVersionEntity entity);

    protected String find(Integer versionId, FileType fileType) {

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionId, fileType);
        String filePath = (fileEntity != null) ? fileEntity.getPath() : null;
        return (filePath != null && fileStorage.isExistContent(filePath)) ? filePath : null;
    }

    private String create(RefBookVersion version, FileType fileType, VersionService versionService) {

        String filePath;
        try (InputStream is = generateVersionFile(version, fileType, versionService)) {
            filePath = fileStorage.saveContent(is, generateZipName(version, fileType));

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

    protected void save(RefBookVersion version, FileType fileType, String filePath) {

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(version.getId(), fileType);
        save(fileEntity, version, fileType, filePath);
    }

    private void save(VersionFileEntity fileEntity,
                      RefBookVersion version, FileType fileType, String filePath) {

        if (fileEntity != null)
            return;

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(version.getId());

        VersionFileEntity newEntity = new VersionFileEntity(versionEntity, fileType, filePath);
        versionFileRepository.save(newEntity);
    }

    private ExportFile export(RefBookVersion version, FileType fileType, String filePath) {

        return new ExportFile(
                fileStorage.getContent(filePath),
                generateZipName(version, fileType)
        );
    }

    protected abstract String generateZipName(RefBookVersion version, FileType fileType);
}
