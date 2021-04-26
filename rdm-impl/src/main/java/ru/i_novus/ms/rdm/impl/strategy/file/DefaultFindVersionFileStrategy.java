package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

@Component
public class DefaultFindVersionFileStrategy implements FindVersionFileStrategy {

    @Autowired
    private VersionFileRepository versionFileRepository;

    @Autowired
    private FileStorage fileStorage;

    @Override
    public String find(Integer versionId, FileType fileType) {

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionId, fileType);
        String filePath = (fileEntity != null) ? fileEntity.getPath() : null;
        return (filePath != null && fileStorage.isExistContent(filePath)) ? filePath : null;
    }
}
