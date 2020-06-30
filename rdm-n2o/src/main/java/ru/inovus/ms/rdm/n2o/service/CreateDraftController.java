package ru.inovus.ms.rdm.n2o.service;

import com.google.common.collect.ImmutableSet;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
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
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@Controller
@SuppressWarnings("unused")
public class CreateDraftController {

    private static final String ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE = "action.draft.was.changed";
    private static final String VERSION_IS_NOT_DRAFT_EXCEPTION_CODE = "version.is.not.draft";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE = "version.has.not.structure";
    private static final String UPDATED_DATA_NOT_FOUND_IN_CURRENT_EXCEPTION_CODE = "updated.data.not.found.in.current";
    private static final String UPDATED_DATA_NOT_FOUND_IN_DRAFT_EXCEPTION_CODE = "updated.data.not.found.in.draft";
    private static final String DATA_ROW_IS_EMPTY_EXCEPTION_CODE = "data.row.is.empty";
    private static final String DATA_ROW_PK_EXISTS_EXCEPTION_CODE = "data.row.pk.exists";

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

    public UiDraft editPassport(Integer versionId, UiPassport uiPassport, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (isVersionDraft(versionId, uiDraft)) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
        }

        refBookService.update(toRefBookUpdateRequest(uiDraft.getId(), uiPassport, optLockValue));
        return uiDraft;
    }

    private RefBookUpdateRequest toRefBookUpdateRequest(Integer versionId, UiPassport uiPassport, Integer optLockValue) {

        final RefBookUpdateRequest request = new RefBookUpdateRequest();
        request.setVersionId(versionId);
        request.setCode(uiPassport.getCode());
        request.setCategory(uiPassport.getCategory());

        Map<String, String> passport = toPassport(uiPassport);
        request.setPassport(passport);

        request.setOptLockValue(optLockValue);

        return request;
    }

    private Map<String, String> toPassport(UiPassport uiPassport) {
        
        if (uiPassport == null)
            return null;

        Map<String, String> passport = new HashMap<>();

        passport.put("name", uiPassport.getName());
        passport.put("shortName", uiPassport.getShortName());
        passport.put("description", uiPassport.getDescription());

        return passport;
    }

    public UiDraft createAttribute(Integer versionId, FormAttribute formAttribute, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (isVersionDraft(versionId, uiDraft)) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
        }

        structureController.createAttribute(uiDraft.getId(), formAttribute, optLockValue);
        return uiDraft;
    }

    public UiDraft updateAttribute(Integer versionId, FormAttribute formAttribute, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (isVersionDraft(versionId, uiDraft)) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
        }

        structureController.updateAttribute(uiDraft.getId(), formAttribute, optLockValue);
        return uiDraft;
    }

    public UiDraft deleteAttribute(Integer versionId, String attributeCode, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (isVersionDraft(versionId, uiDraft)) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
        }

        structureController.deleteAttribute(uiDraft.getId(), attributeCode, optLockValue);
        return uiDraft;
    }

    public UiDraft updateDataRecord(Integer versionId, Row row, Integer optLockValue) {

        validatePresent(row);

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (isVersionDraft(versionId, uiDraft)) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            // Изменение записи в опубликованной версии:
            optLockValue = null; // Новый справочник, поэтому блокировки нет (см. также в других методах).
            row.setSystemId(findNewSystemId(row.getSystemId(), versionId, uiDraft.getId()));
        }

        validatePrimaryKeys(uiDraft.getId(), row);

        dataRecordController.updateData(uiDraft.getId(), row, optLockValue);
        return uiDraft;
    }

    /** Проверка на заполненность хотя бы одного поля в записи. */
    private void validatePresent(Row row) {
        if (RowUtils.isEmptyRow(row))
            throw new UserException(DATA_ROW_IS_EMPTY_EXCEPTION_CODE);
    }

    /**
     * Проверка добавляемой записи на уникальность по первичным ключам в таблице БД.
     *
     * @param versionId идентификатор версии-черновика
     * @param row       проверяемая запись
     */
    private void validatePrimaryKeys(Integer versionId, Row row) {

        if (row.getSystemId() != null)
            return;

        Structure structure = versionService.getStructure(versionId);
        List<Structure.Attribute> primaries = structure.getPrimary();
        if (primaries.isEmpty())
            return;

        List<AttributeFilter> primaryFilters = RowUtils.getPrimaryKeyValueFilters(row, primaries);
        if (primaryFilters.isEmpty())
            return;

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(1);
        criteria.setAttributeFilter(Set.of(primaryFilters));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        if (rowValues != null && !isEmpty(rowValues.getContent())) {
            Message message = new Message(DATA_ROW_PK_EXISTS_EXCEPTION_CODE,
                    RowUtils.toNamedValues(row.getData(), primaries));
            throw new UserException(message);
        }
    }

    public UiDraft deleteDataRecord(Integer versionId, Long sysRecordId, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (isVersionDraft(versionId, uiDraft)) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
            sysRecordId = findNewSystemId(sysRecordId, versionId, uiDraft.getId());
        }

        draftService.deleteRow(uiDraft.getId(), new Row(sysRecordId, emptyMap()), optLockValue);
        return uiDraft;
    }

    public UiDraft deleteAllDataRecords(Integer versionId, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (isVersionDraft(versionId, uiDraft)) {
            validateOptLockValue(uiDraft, optLockValue);
        } else {
            optLockValue = null;
        }

        draftService.deleteAllRows(uiDraft.getId(), optLockValue);
        return uiDraft;
    }

    /** Поиск идентификатора записи в черновике по старому идентификатору в текущей версии. */
    private Long findNewSystemId(Long oldSystemId, Integer oldVersionId, Integer newVersionId) {

        if (oldSystemId == null) return null;

        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordIdFilter = new AttributeFilter(DataConstants.SYS_PRIMARY_COLUMN, oldSystemId.intValue(), FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordIdFilter)));

        Page<RefBookRowValue> oldRow = versionService.search(oldVersionId, criteria);
        if (isEmpty(oldRow.getContent()))
            throw new NotFoundException(UPDATED_DATA_NOT_FOUND_IN_CURRENT_EXCEPTION_CODE);

        String hash = oldRow.getContent().get(0).getHash();
        AttributeFilter hashFilter = new AttributeFilter(DataConstants.SYS_HASH, hash, FieldType.STRING);
        final SearchDataCriteria hashCriteria = new SearchDataCriteria(ImmutableSet.of(singletonList(hashFilter)), null);

        final Page<RefBookRowValue> newRow = versionService.search(newVersionId, hashCriteria);
        if (isEmpty(newRow.getContent()))
            throw new NotFoundException(UPDATED_DATA_NOT_FOUND_IN_DRAFT_EXCEPTION_CODE);

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

        if (version.hasEmptyStructure())
            throw new UserException(new Message(VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE, versionId));

        draftService.updateData(versionId, fileModel);

        return new UiDraft(versionId, version.getRefBookId(), version.getOptLockValue());
    }

    private boolean isVersionDraft(Integer versionId, UiDraft uiDraft) {
        return Objects.equals(versionId, uiDraft.getId());
    }

    private void validateOptLockValue(UiDraft uiDraft, Integer optLockValue) {

        if (optLockValue != null && !optLockValue.equals(uiDraft.getOptLockValue())) {
            throw new UserException(new Message(ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE));
        }
    }
}
