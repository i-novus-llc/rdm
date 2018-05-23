package ru.inovus.ms.mdm.service;

import ru.inovus.ms.mdm.model.Data;
import ru.inovus.ms.mdm.model.Metadata;
import ru.inovus.ms.mdm.model.VersionCriteria;

public interface VersionService {

    Data search(Long versionId, VersionCriteria criteria);
    Metadata getMetadata(Long versionId);
}
