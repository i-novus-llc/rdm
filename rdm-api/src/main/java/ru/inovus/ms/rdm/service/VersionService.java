package ru.inovus.ms.rdm.service;

import ru.inovus.ms.rdm.model.Data;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.VersionCriteria;

public interface VersionService {

    Data search(Long versionId, VersionCriteria criteria);
    Structure getMetadata(Long versionId);
}
