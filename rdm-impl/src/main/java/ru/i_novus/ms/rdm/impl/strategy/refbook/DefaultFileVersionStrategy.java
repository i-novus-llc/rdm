package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

@Component
@Primary
public class DefaultFileVersionStrategy implements FileVersionStrategy {

    @Autowired
    private VersionFileRepository versionFileRepository;

    @Override
    public void save(RefBookVersion versionModel, FileType fileType, String path) {
        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionModel.getId(), fileType);
        saveNewVersion(fileEntity, versionModel, fileType, path);
    }

    protected void saveNewVersion(VersionFileEntity fileEntity,
                                  RefBookVersion versionModel, FileType fileType, String path) {
        if (fileEntity == null && !versionModel.isDraft()) {
            RefBookVersionEntity versionEntity = new RefBookVersionEntity();
            versionEntity.setId(versionModel.getId());

            fileEntity = new VersionFileEntity(versionEntity, fileType, path);
            versionFileRepository.save(fileEntity);
        }
    }
}
