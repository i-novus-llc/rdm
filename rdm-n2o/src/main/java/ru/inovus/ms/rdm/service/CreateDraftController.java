package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Objects;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@Controller
public class CreateDraftController {

    private DraftService draftService;
    private RefBookService refBookService;
    private VersionService versionService;
    private StructureController structureController;
    private DataRecordController dataRecordController;

    @Autowired
    public CreateDraftController(DraftService draftService, RefBookService refBookService, VersionService versionService,
                                 StructureController structureController, DataRecordController dataRecordController) {
        this.draftService = draftService;
        this.refBookService = refBookService;
        this.versionService = versionService;
        this.structureController = structureController;
        this.dataRecordController = dataRecordController;
    }

    private UiDraft getOrCreateDraft(Integer versionId) {
        final RefBookVersion version = versionService.getById(versionId);
        if (RefBookVersionStatus.DRAFT.equals(version.getStatus()))
            return new UiDraft(versionId, version.getRefBookId());
        else
            return new UiDraft(draftService.createFromVersion(versionId).getId(), version.getRefBookId());
    }

    UiDraft editPassport(RefBookUpdateRequest refBookUpdateRequest) {
        final UiDraft uiDraft = getOrCreateDraft(refBookUpdateRequest.getVersionId());
        Integer draftId = uiDraft.getId();
        refBookUpdateRequest.setVersionId(draftId);
        refBookService.update(refBookUpdateRequest);
        return uiDraft;
    }

    UiDraft createAttribute(Integer versionId, Attribute attribute) {
        final UiDraft uiDraft = getOrCreateDraft(versionId);
        Integer draftId = uiDraft.getId();
        structureController.createAttribute(draftId, attribute);
        return uiDraft;
    }

    UiDraft updateAttribute(Integer versionId, Attribute attribute) {
        final UiDraft uiDraft = getOrCreateDraft(versionId);
        Integer draftId = uiDraft.getId();
        structureController.updateAttribute(draftId, attribute);
        return uiDraft;
    }

    UiDraft deleteAttribute(Integer versionId, String attributeCode) {
        final UiDraft uiDraft = getOrCreateDraft(versionId);
        Integer draftId = uiDraft.getId();
        draftService.deleteAttribute(draftId, attributeCode);
        return uiDraft;
    }

    UiDraft updateDataRecord(Integer versionId, Row row) {

        UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!Objects.equals(versionId, uiDraft.getId())) {
            row.setSystemId(calculateNewSystemId(row.getSystemId(), versionId, uiDraft.getId()));
        }
        dataRecordController.updateData(uiDraft.getId(), row);
        return uiDraft;

    }

    UiDraft deleteDataRecord(Integer versionId, Long systemId) {

        UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!Objects.equals(versionId, uiDraft.getId())) {
            systemId = calculateNewSystemId(systemId, versionId, uiDraft.getId());
        }
        draftService.deleteRow(uiDraft.getId(), systemId);
        return uiDraft;
    }

    private Long calculateNewSystemId(Long oldSystemId, Integer oldVersionId, Integer newVersionId) {
        if (oldSystemId == null) return null;
        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordId = new AttributeFilter("SYS_RECORDID", oldSystemId.intValue(), FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordId)));
        Page<RefBookRowValue> oldRow = versionService.search(oldVersionId, criteria);
        if (CollectionUtils.isEmpty(oldRow.getContent())) throw new NotFoundException("record not found");

        String hash = oldRow.getContent().get(0).getHash();

        final SearchDataCriteria hashCriteria = new SearchDataCriteria(of(singletonList(
                new AttributeFilter("SYS_HASH", hash, FieldType.STRING))), null);
        final Page<RefBookRowValue> newRow = versionService.search(newVersionId, hashCriteria);
        if (CollectionUtils.isEmpty(newRow.getContent())) throw new NotFoundException("record not found");
        return newRow.getContent().get(0).getSystemId();
    }

    public UiDraft uploadFromFile(Integer versionId, FileModel fileModel) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message("version.not.found", versionId));

        if (RefBookVersionStatus.DRAFT.equals(version.getStatus()) ) {
            draftService.updateData(versionId, fileModel);
        } else {
            draftService.create(version.getRefBookId(), version.getStructure());
            versionId = draftService.create(version.getRefBookId(), fileModel).getId();
        }

        return new UiDraft(versionId, version.getRefBookId());
    }

}
