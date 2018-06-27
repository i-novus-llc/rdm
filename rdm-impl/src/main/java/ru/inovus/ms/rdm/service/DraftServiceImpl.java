package ru.inovus.ms.rdm.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
public class DraftServiceImpl implements DraftService {

    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    @Override
    public Draft create(Long dictionaryId, Structure structure) {
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
    public void publish(Integer draftId, String versionName, OffsetDateTime versionDate) {
        RefBookVersionEntity draftVersion = versionRepository.findOne(draftId);
        Page<RefBookVersionEntity> lastPublishedVersions = versionRepository
                .findAll(isPublished().and(isVersionOfRefBook(draftVersion.getRefBook().getId()))
                        , new PageRequest(1, 1, new Sort(Sort.Direction.ASC, "title")));
        RefBookVersionEntity lastPublishedVersion = lastPublishedVersions != null && !lastPublishedVersions.hasContent() ? lastPublishedVersions.getContent().get(0) : null;
        String storageCode = draftDataService.applyDraft(
                lastPublishedVersion != null ? lastPublishedVersion.getStorageCode() : null,
                draftVersion.getStorageCode(),
                new Date(versionDate.toInstant().toEpochMilli())
        );
        if(lastPublishedVersion == null) {
            draftVersion.setVersion("1.0");
        }
        draftVersion.setStorageCode(storageCode);
        draftVersion.setVersion(versionName);
        draftVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        versionRepository.save(draftVersion);
    }

    @Override
    public void remove(Long draftId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Structure getMetadata(Long draftId) {
        return null;
    }
}
