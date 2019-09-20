package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.inovus.ms.rdm.impl.entity.VersionFileEntity;
import ru.inovus.ms.rdm.api.enumeration.FileType;

/**
 * Created by znurgaliev on 08.08.2018.
 */
public interface VersionFileRepository extends
        JpaRepository<VersionFileEntity, Integer> {

    VersionFileEntity findByVersionIdAndType(Integer versionId, FileType fileType);

}
