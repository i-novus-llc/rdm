package ru.inovus.ms.rdm.service;

import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.VersionCriteria;

public interface VersionService {

    Page<RowValue> search(Integer versionId, VersionCriteria criteria);
    Structure getMetadata(Integer versionId);
}
