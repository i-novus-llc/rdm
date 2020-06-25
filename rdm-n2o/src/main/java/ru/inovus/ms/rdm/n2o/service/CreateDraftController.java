package ru.inovus.ms.rdm.n2o.service;

import com.google.common.collect.ImmutableSet;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DataConstants;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.util.RowUtils;
import ru.inovus.ms.rdm.n2o.model.FormAttribute;
import ru.inovus.ms.rdm.n2o.model.UiDraft;
import ru.inovus.ms.rdm.n2o.model.UiPassport;

import java.util.*;

import static java.util.Collections.*;

@Controller
@SuppressWarnings("unused")
public class CreateDraftController {

    private static final String ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE = "action.draft.was.changed";
    private static final String VERSION_IS_NOT_DRAFT_EXCEPTION_CODE = "version.is.not.draft";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE = "version.has.not.structure";

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
        final Integer refBookId = version.getRefBookId();
        
        if (version.isDraft()) {
            return new UiDraft(versionId, refBookId, version.getOptLockValue());
        }

        Integer draftId = draftService.getIdByRefBookCode(version.getCode());
        if (draftId != null) {
            Draft draft = draftService.getDraft(draftId);
            return new UiDraft(draftId, refBookId, draft.getOptLockValue());
        }

        Draft newDraft = draftService.createFromVersion(versionId);
        return new UiDraft(newDraft.getId(), refBookId, newDraft.getOptLockValue());
    }

    public UiDraft editPassport(Integer versionId, UiPassport uiPassport) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);
        refBookService.update(toRefBookUpdateRequest(uiDraft.getId(), uiPassport));
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

    public UiDraft createAttribute(Integer versionId, FormAttribute formAttribute) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);
        structureController.createAttribute(uiDraft.getId(), formAttribute);
        return uiDraft;
    }

    public UiDraft updateAttribute(Integer versionId, FormAttribute formAttribute) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);
        structureController.updateAttribute(uiDraft.getId(), formAttribute);
        return uiDraft;
    }

    public UiDraft deleteAttribute(Integer versionId, String attributeCode) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);
        structureController.deleteAttribute(uiDraft.getId(), attributeCode);
        return uiDraft;
    }

    public UiDraft updateDataRecord(Integer versionId, Row row, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (Objects.equals(versionId, uiDraft.getId())) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
            row.setSystemId(calculateNewSystemId(row.getSystemId(), versionId, uiDraft.getId()));
        }

//      Значит была нажата кнопка "Добавить строку".
//      Если добавят строку с существующим первичным ключом, ошибки не будет, строка просто обновится новыми данными.
//      Поэтому надо самим проверить, есть ли уже такой первичный ключ в справочнике и если есть -- бросить ошибку.
        if (row.getSystemId() == null) {
            validateCreatedRowExist(uiDraft.getId(), row);
        }

        dataRecordController.updateData(uiDraft.getId(), row, optLockValue);
        return uiDraft;
    }

    private void validateCreatedRowExist(Integer draftId, Row row) {

        RefBookVersion refBookVersion = versionService.getById(draftId);
        List<Structure.Attribute> primary = refBookVersion.getStructure().getPrimary();
        if (primary.isEmpty())
            return;

        List<AttributeFilter> primaryKeyValueFilters = RowUtils.getPrimaryKeyValueFilters(row, primary);
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(Set.of(primaryKeyValueFilters), null);
        searchDataCriteria.setPageSize(1);
        searchDataCriteria.setPageNumber(1);

        boolean exists = versionService.search(draftId, searchDataCriteria).getTotalElements() > 0;
        if (exists)
            throw new UserException("pk.is.already.exists");
    }

    public UiDraft deleteDataRecord(Integer versionId, Long sysRecordId, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (Objects.equals(versionId, uiDraft.getId())) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
            sysRecordId = calculateNewSystemId(sysRecordId, versionId, uiDraft.getId());
        }

        draftService.deleteRow(uiDraft.getId(), new Row(sysRecordId, emptyMap()));
        return uiDraft;
    }

    public UiDraft deleteAllDataRecords(Integer versionId, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (Objects.equals(versionId, uiDraft.getId())) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
        }

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

    public UiDraft createFromFile(FileModel fileModel) {

        Integer versionId = refBookService.create(fileModel).getId();
        RefBookVersion version = versionService.getById(versionId);

        return new UiDraft(versionId, version.getRefBookId(), version.getOptLockValue());
    }

    public UiDraft uploadFromFile(Integer versionId, FileModel fileModel) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));

        if (!version.isDraft())
            throw new UserException(new Message(VERSION_IS_NOT_DRAFT_EXCEPTION_CODE, versionId));

        Draft draft = draftService.create(version.getRefBookId(), fileModel);
        versionId = draft.getId();

        return new UiDraft(versionId, version.getRefBookId(), draft.getOptLockValue());
    }

    public UiDraft uploadData(Integer versionId, FileModel fileModel) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));

        if (!version.isDraft())
            throw new UserException(new Message(VERSION_IS_NOT_DRAFT_EXCEPTION_CODE, versionId));

        if (version.getStructure() == null || version.getStructure().isEmpty())
            throw new UserException(new Message(VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE, versionId));

        draftService.updateData(versionId, fileModel);

        return new UiDraft(versionId, version.getRefBookId(), version.getOptLockValue());
    }

    private void validateOptLockValue(UiDraft uiDraft, Integer optLockValue) {

        if (optLockValue != null && !optLockValue.equals(uiDraft.getOptLockValue())) {
            throw new UserException(new Message(ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE));
        }
    }
}
