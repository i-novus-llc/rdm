package ru.inovus.ms.rdm.service;

import com.google.common.collect.ImmutableSet;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.version.AttributeFilter;
import ru.inovus.ms.rdm.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.refdata.Row;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@Controller
public class CreateDraftController {

    private RefBookService refBookService;
    private VersionService versionService;
    private DraftService draftService;

    private StructureController structureController;
    private DataRecordController dataRecordController;

    @Autowired
    public CreateDraftController(RefBookService refBookService, VersionService versionService, DraftService draftService,
                                 StructureController structureController, DataRecordController dataRecordController) {
        this.refBookService = refBookService;
        this.versionService = versionService;
        this.draftService = draftService;

        this.structureController = structureController;
        this.dataRecordController = dataRecordController;
    }

    private UiDraft getOrCreateDraft(Integer versionId) {
        final RefBookVersion version = versionService.getById(versionId);
        if (version.isDraft())
            return new UiDraft(versionId, version.getRefBookId());
        else
            return new UiDraft(draftService.createFromVersion(versionId).getId(), version.getRefBookId());
    }

    @SuppressWarnings("unused")
    UiDraft editPassport(Integer versionId, UiPassport uiPassport) {
        final UiDraft uiDraft = getOrCreateDraft(versionId);
        Integer draftId = uiDraft.getId();
        refBookService.update(toRefBookUpdateRequest(draftId, uiPassport));
        return uiDraft;
    }

    private RefBookUpdateRequest toRefBookUpdateRequest(Integer versionId, UiPassport uiPassport) {
        final RefBookUpdateRequest refBookUpdateRequest = new RefBookUpdateRequest();
        refBookUpdateRequest.setVersionId(versionId);
        refBookUpdateRequest.setCode(uiPassport.getCode());
        refBookUpdateRequest.setCategory(uiPassport.getCategory());
        Map<String, String> passport = new HashMap<>();
        passport.put("name", uiPassport.getName());
        passport.put("shortName", uiPassport.getShortName());
        passport.put("description", uiPassport.getDescription());
        refBookUpdateRequest.setPassport(passport);
        return refBookUpdateRequest;
    }

    @SuppressWarnings("unused")
    UiDraft createAttribute(Integer versionId, FormAttribute formAttribute) {
        final UiDraft uiDraft = getOrCreateDraft(versionId);
        Integer draftId = uiDraft.getId();
        structureController.createAttribute(draftId, formAttribute);
        return uiDraft;
    }

    @SuppressWarnings("unused")
    UiDraft updateAttribute(Integer versionId, FormAttribute formAttribute) {
        final UiDraft uiDraft = getOrCreateDraft(versionId);
        Integer draftId = uiDraft.getId();
        structureController.updateAttribute(draftId, formAttribute);
        return uiDraft;
    }

    @SuppressWarnings("unused")
    UiDraft deleteAttribute(Integer versionId, String attributeCode) {
        final UiDraft uiDraft = getOrCreateDraft(versionId);
        Integer draftId = uiDraft.getId();
        draftService.deleteAttribute(draftId, attributeCode);
        return uiDraft;
    }

    @SuppressWarnings("unused")
    UiDraft updateDataRecord(Integer versionId, Row row) {

        UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!Objects.equals(versionId, uiDraft.getId())) {
            row.setSystemId(calculateNewSystemId(row.getSystemId(), versionId, uiDraft.getId()));
        }
        dataRecordController.updateData(uiDraft.getId(), row);
        return uiDraft;

    }

    @SuppressWarnings("unused")
    UiDraft deleteDataRecord(Integer versionId, Long systemId) {

        UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!Objects.equals(versionId, uiDraft.getId())) {
            systemId = calculateNewSystemId(systemId, versionId, uiDraft.getId());
        }
        draftService.deleteRow(uiDraft.getId(), systemId);
        return uiDraft;
    }

    @SuppressWarnings("unused")
    UiDraft deleteAllDataRecords(Integer versionId) {

        UiDraft uiDraft = getOrCreateDraft(versionId);

        draftService.deleteAllRows(uiDraft.getId());
        return uiDraft;
    }

    private Long calculateNewSystemId(Long oldSystemId, Integer oldVersionId, Integer newVersionId) {
        if (oldSystemId == null) return null;

        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordIdFilter = new AttributeFilter(DataConstants.SYS_PRIMARY_COLUMN, oldSystemId.intValue(), FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordIdFilter)));

        Page<RefBookRowValue> oldRow = versionService.search(oldVersionId, criteria);
        if (CollectionUtils.isEmpty(oldRow.getContent())) throw new NotFoundException("record not found");
        String hash = oldRow.getContent().get(0).getHash();

        AttributeFilter hashFilter = new AttributeFilter(DataConstants.SYS_HASH, hash, FieldType.STRING);
        final SearchDataCriteria hashCriteria = new SearchDataCriteria(ImmutableSet.of(singletonList(hashFilter)), null);

        final Page<RefBookRowValue> newRow = versionService.search(newVersionId, hashCriteria);
        if (CollectionUtils.isEmpty(newRow.getContent())) throw new NotFoundException("record not found");
        return newRow.getContent().get(0).getSystemId();
    }

    @SuppressWarnings("unused")
    public UiDraft createFromFile(FileModel fileModel) {

        Integer versionId = draftService.create(fileModel).getId();
        RefBookVersion version = versionService.getById(versionId);

        return new UiDraft(versionId, version.getRefBookId());
    }

    @SuppressWarnings("unused")
    public UiDraft uploadFromFile(Integer versionId, FileModel fileModel) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message("version.not.found", versionId));

        versionId = draftService.create(version.getRefBookId(), fileModel).getId();

        return new UiDraft(versionId, version.getRefBookId());
    }

    @SuppressWarnings("unused")
    public UiDraft uploadData(Integer versionId, FileModel fileModel) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message("version.not.found", versionId));

        if (!version.isDraft())
            throw new UserException(new Message("version.is.not.draft", versionId));

        if (version.getStructure() == null
                || CollectionUtils.isEmpty(version.getStructure().getAttributes()))
            throw new UserException(new Message("version.has.not.structure", versionId));

        draftService.updateData(versionId, fileModel);

        return new UiDraft(versionId, version.getRefBookId());
    }

}
