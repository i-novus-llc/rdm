package ru.inovus.ms.rdm.service;

import ru.inovus.ms.rdm.model.Data;
import ru.inovus.ms.rdm.model.Metadata;
import ru.inovus.ms.rdm.model.VersionCriteria;

public interface VersionService {

    Data search(Long versionId, VersionCriteria criteria);
    Metadata getMetadata(Long versionId);
}
