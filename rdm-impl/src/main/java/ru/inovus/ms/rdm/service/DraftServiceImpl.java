package ru.inovus.ms.rdm.service;

import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.*;

import java.time.OffsetDateTime;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
public class DraftServiceImpl implements DraftService {

    @Override
    public Draft create(Long dictionaryId, Metadata metadata) {
        return null;
    }

    @Override
    public void updateMetadata(Long draftId, MetadataDiff metadataDiff) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateData(Long draftId, DataDiff dataDiff) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateData(Long draftId, FileData file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Data search(Long draftId, DraftCriteria criteria) {
        return null;
    }

    @Override
    public void publish(Long draftId, String versionName, OffsetDateTime versionDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Long draftId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Metadata getMetadata(Long draftId) {
        return null;
    }
}
