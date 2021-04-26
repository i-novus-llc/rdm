package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

public class DefaultFindVersionFileStrategy implements FindVersionFileStrategy {

    @Autowired
    private VersionFileRepository versionFileRepository;

    @Override
    public String find(Integer versionId, FileType fileType) {

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionId, fileType);
        return (fileEntity != null) ? fileEntity.getPath() : null;
    }
}
