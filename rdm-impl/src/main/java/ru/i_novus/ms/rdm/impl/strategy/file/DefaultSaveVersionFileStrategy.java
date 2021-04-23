package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

@Component
public class DefaultSaveVersionFileStrategy implements SaveVersionFileStrategy {

    @Autowired
    private VersionFileRepository versionFileRepository;

    @Override
    public void save(RefBookVersion version, FileType fileType, String path) {

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(version.getId(), fileType);

        save(fileEntity, version, fileType, path);
    }

    protected void save(VersionFileEntity fileEntity,
                        RefBookVersion version, FileType fileType, String path) {

        if (fileEntity != null || version.isDraft())
            return;

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(version.getId());

        VersionFileEntity newEntity = new VersionFileEntity(versionEntity, fileType, path);
        versionFileRepository.save(newEntity);
    }
}
