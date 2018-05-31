package ru.inovus.ms.rdm.service;

import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.Data;
import ru.inovus.ms.rdm.model.Metadata;
import ru.inovus.ms.rdm.model.VersionCriteria;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
public class VersionServiceImpl implements VersionService {
    @Override
    public Data search(Long versionId, VersionCriteria criteria) {
        return null;
    }

    @Override
    public Metadata getMetadata(Long versionId) {
        return null;
    }
}
