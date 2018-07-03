package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.RowValuePage;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isPublished;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isVersionOfRefBook;
import static ru.inovus.ms.rdm.util.ConverterUtil.structureToFields;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
public class DraftServiceImpl implements DraftService {

    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    private FieldFactory fieldFactory;

    private SearchDataService searchDataService;

    private DropDataService dropDataService;

    private RefBookRepository refBookRepository;

    @Autowired
    public DraftServiceImpl(DraftDataService draftDataService, RefBookVersionRepository versionRepository, FieldFactory fieldFactory,
                            RefBookRepository refBookRepository, SearchDataService searchDataService, DropDataService dropDataService) {
        this.draftDataService = draftDataService;
        this.versionRepository = versionRepository;
        this.fieldFactory = fieldFactory;
        this.searchDataService = searchDataService;
        this.dropDataService = dropDataService;
        this.refBookRepository = refBookRepository;
    }


    @Override
    @Transactional
    public Draft create(Integer refBookId, Structure structure) {
        // достать существующий draftVersion по refBookId  и проапдейтить и если старая и новая метада отличается то удалить старый draftCode DropDataService.
        // А если совпадают то удалить данные в нем через ru.i_novus.platform.datastorage.temporal.service.DraftDataService.deleteAllRows

        RefBookVersionEntity lastRefBookVersion = getLastRefBookVersion(refBookId);
        RefBookVersionEntity draftVersion = getDraftByRefbook(refBookId);
        if (draftVersion == null && lastRefBookVersion == null) {
            throw new RuntimeException("invalid refbook");
        }
        if (draftVersion == null) {
            // create
            draftVersion = new RefBookVersionEntity();
            draftVersion.setStatus(RefBookVersionStatus.DRAFT);
            draftVersion.setFullName(lastRefBookVersion.getFullName());
            draftVersion.setShortName(lastRefBookVersion.getShortName());
            draftVersion.setAnnotation(lastRefBookVersion.getAnnotation());
            draftVersion.setStructure(structure);
            List<Field> fields = structureToFields(structure, fieldFactory);
            draftVersion.setRefBook(refBookRepository.findOne(refBookId));
            String draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);
        } else {
            updateDraft(structure, draftVersion);
        }
        RefBookVersionEntity savedDraftVersion = versionRepository.save(draftVersion);
        return new Draft(savedDraftVersion.getId(), savedDraftVersion.getStorageCode());
    }

    private RefBookVersionEntity getDraftByRefbook(Integer refBookId) {
        return versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookId);
    }

    private void updateDraft(Structure structure, RefBookVersionEntity draftVersion) {
        List<Field> fields = structureToFields(structure, fieldFactory);
        String draftCode = draftVersion.getStorageCode();
        if (!structure.equals(draftVersion.getStructure())) {
            dropDataService.drop(Collections.singleton(draftCode));
            draftCode = draftDataService.createDraft(fields);
            draftVersion.setStorageCode(draftCode);
        } else {
            draftDataService.deleteAllRows(draftCode);
        }
        draftVersion.setStructure(structure);
    }


    @Override
    public void updateMetadata(Integer draftId, MetadataDiff metadataDiff) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateData(Integer draftId, DataDiff dataDiff) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateData(Integer draftId, FileData file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<RowValue> search(Integer draftId, DraftCriteria criteria) {
        RefBookVersionEntity draft = versionRepository.findOne(draftId);
        String storageCode = draft.getStorageCode();
        List<Field> fields = structureToFields(draft.getStructure(), fieldFactory);
        DataCriteria dataCriteria = new DataCriteria(storageCode, null, null,
                fields, criteria.getFieldFilter(), criteria.getCommonFilter());
        //fields - все поля из draft. fieldFilter и commonFilter из DraftCriteria
        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return new RowValuePage(pagedData);
    }



    @Override
    public void publish(Integer draftId, String versionName, OffsetDateTime versionDate) {
        RefBookVersionEntity draftVersion = versionRepository.findOne(draftId);
        RefBookVersionEntity lastPublishedVersion = getLastRefBookVersion(draftVersion.getRefBook().getId());
        String storageCode = draftDataService.applyDraft(
                lastPublishedVersion != null ? lastPublishedVersion.getStorageCode() : null,
                draftVersion.getStorageCode(),
                new Date(versionDate.toInstant().toEpochMilli())
        );
        if (lastPublishedVersion == null) {
            draftVersion.setVersion("1.0");
        }
        draftVersion.setStorageCode(storageCode);
        draftVersion.setVersion(versionName);
        draftVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        versionRepository.save(draftVersion);
    }

    private RefBookVersionEntity getLastRefBookVersion(Integer refBookId) {
        Page<RefBookVersionEntity> lastPublishedVersions = versionRepository
                .findAll(isPublished().and(isVersionOfRefBook(refBookId))
                        , new PageRequest(1, 1, new Sort(Sort.Direction.DESC, "fromDate")));
        return lastPublishedVersions != null && lastPublishedVersions.hasContent() ? lastPublishedVersions.getContent().get(0) : null;
    }

    @Override
    public void remove(Integer draftId) {
        versionRepository.delete(draftId);
    }

    @Override
    public Structure getMetadata(Integer draftId) {
        return null;
    }

    @Override
    public Draft getDraft(Integer draftId) {
        RefBookVersionEntity versionEntity = versionRepository.findOne(draftId);
        return versionEntity != null ? new Draft(versionEntity.getId(), versionEntity.getStorageCode()) : null;
    }
}
