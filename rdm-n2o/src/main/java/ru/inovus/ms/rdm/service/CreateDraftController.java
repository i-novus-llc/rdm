package ru.inovus.ms.rdm.service;

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
    public CreateDraftController(DraftService draftService, RefBookService refBookService, VersionService versionService, StructureController structureController, DataRecordController dataRecordController) {
        this.draftService = draftService;
        this.refBookService = refBookService;
        this.versionService = versionService;
        this.structureController = structureController;
        this.dataRecordController = dataRecordController;
    }




    private boolean isDraft(Integer versionId) {
        final RefBookVersion version = versionService.getById(versionId);
        return RefBookVersionStatus.DRAFT.equals(version.getStatus());
    }

    private Integer getDraftId(Integer versionId) {
        if (isDraft(versionId))
            return versionId;
        else
            return draftService.createFromVersion(versionId).getId();
    }

    Draft editPassport(RefBookUpdateRequest refBookUpdateRequest) {
        Integer draftId = getDraftId(refBookUpdateRequest.getVersionId());
        refBookUpdateRequest.setVersionId(draftId);
        refBookService.update(refBookUpdateRequest);
        return new Draft(draftId, null);
    }

    Draft createAttribute(Integer versionId, Attribute attribute) {
        Integer draftId = getDraftId(versionId);
        structureController.createAttribute(draftId, attribute);
        return new Draft(draftId, null);
    }

    Draft updateAttribute(Integer versionId, Attribute attribute) {
        Integer draftId = getDraftId(versionId);
        structureController.updateAttribute(draftId, attribute);
        return new Draft(draftId, null);
    }

    Draft deleteAttribute(Integer versionId, String attributeCode) {
        Integer draftId = getDraftId(versionId);
        draftService.deleteAttribute(draftId, attributeCode);
        return new Draft(draftId, null);
    }

    Draft updateDataRecord(Integer versionId, Row row) {
        Integer draftId;
        if (isDraft(versionId))
            draftId = versionId;
        else {
            draftId = draftService.createFromVersion(versionId).getId();
            row.setSystemId(calculateNewSystemId(row.getSystemId(), versionId, draftId));
        }
        dataRecordController.updateData(draftId, row);
        return new Draft(draftId, null);
    }

    Draft deleteDataRecord(Integer versionId, Long systemId) {
        Integer draftId;
        Long rowId;
        if (isDraft(versionId)){
            draftId = versionId;
            rowId = systemId;
        }
        else {
            draftId = draftService.createFromVersion(versionId).getId();
            rowId = calculateNewSystemId(systemId, versionId, draftId);
        }
        draftService.deleteRow(draftId, rowId);
        return new Draft(draftId, null);
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
        final Page<RefBookRowValue> newRow = versionService.search(oldVersionId, hashCriteria);
        if (CollectionUtils.isEmpty(newRow.getContent())) throw new NotFoundException("record not found");
        return newRow.getContent().get(0).getSystemId();
    }
}
