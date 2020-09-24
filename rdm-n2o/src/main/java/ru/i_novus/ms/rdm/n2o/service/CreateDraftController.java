package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;
import ru.i_novus.ms.rdm.n2o.model.FormAttribute;
import ru.i_novus.ms.rdm.n2o.model.UiPassport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@Controller
@SuppressWarnings("unused") // used in: *.object.xml, *ObjectProvider, *ObjectResolver
public class CreateDraftController {

    private static final String VERSION_IS_NOT_DRAFT_EXCEPTION_CODE = "version.is.not.draft";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE = "version.has.not.structure";
    private static final String UPDATED_DATA_NOT_FOUND_IN_CURRENT_EXCEPTION_CODE = "updated.data.not.found.in.current";
    private static final String UPDATED_DATA_NOT_FOUND_IN_DRAFT_EXCEPTION_CODE = "updated.data.not.found.in.draft";
    private static final String DATA_ROW_IS_EMPTY_EXCEPTION_CODE = "data.row.is.empty";
    private static final String DATA_ROW_PK_EXISTS_EXCEPTION_CODE = "data.row.pk.exists";

    private static final String PASSPORT_REFBOOK_NAME = "name";
    private static final String PASSPORT_REFBOOK_SHORT_NAME = "shortName";
    private static final String PASSPORT_REFBOOK_DESCRIPTION = "description";

    private RefBookService refBookService;
    private VersionRestService versionService;
    private DraftRestService draftService;

    private StructureController structureController;
    private DataRecordController dataRecordController;

    @Autowired
    public CreateDraftController(RefBookService refBookService,
                                 VersionRestService versionService, DraftRestService draftService,
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
            return new UiDraft(version);
        }

        Draft draft = draftService.findDraft(version.getCode());
        if (draft != null) {
            return new UiDraft(draft, refBookId);
        }

        Draft newDraft = draftService.createFromVersion(versionId);
        return new UiDraft(newDraft, refBookId);
    }

    public UiDraft editPassport(Integer versionId, Integer optLockValue, UiPassport uiPassport) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!uiDraft.isVersionDraft(versionId)) {
            optLockValue = uiDraft.getOptLockValue();
        }

        refBookService.update(toRefBookUpdateRequest(uiDraft.getId(), optLockValue, uiPassport));
        return uiDraft;
    }

    private RefBookUpdateRequest toRefBookUpdateRequest(Integer versionId,
                                                        Integer optLockValue,
                                                        UiPassport uiPassport) {

        final RefBookUpdateRequest request = new RefBookUpdateRequest();
        request.setVersionId(versionId);
        request.setOptLockValue(optLockValue);

        request.setCode(uiPassport.getCode());
        request.setCategory(uiPassport.getCategory());

        Map<String, String> passport = toPassport(uiPassport);
        request.setPassport(passport);

        return request;
    }

    private Map<String, String> toPassport(UiPassport uiPassport) {

        if (uiPassport == null)
            return null;

        Map<String, String> passport = new HashMap<>();

        passport.put(PASSPORT_REFBOOK_NAME, uiPassport.getName());
        passport.put(PASSPORT_REFBOOK_SHORT_NAME, uiPassport.getShortName());
        passport.put(PASSPORT_REFBOOK_DESCRIPTION, uiPassport.getDescription());

        return passport;
    }

    public UiDraft createAttribute(Integer versionId, Integer optLockValue, FormAttribute formAttribute) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!uiDraft.isVersionDraft(versionId)) {
            optLockValue = uiDraft.getOptLockValue();
        }

        structureController.createAttribute(uiDraft.getId(), optLockValue, formAttribute);
        return uiDraft;
    }

    public UiDraft updateAttribute(Integer versionId, Integer optLockValue, FormAttribute formAttribute) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!uiDraft.isVersionDraft(versionId)) {
            optLockValue = uiDraft.getOptLockValue();
        }

        structureController.updateAttribute(uiDraft.getId(), optLockValue, formAttribute);
        return uiDraft;
    }

    public UiDraft deleteAttribute(Integer versionId, Integer optLockValue, String attributeCode) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!uiDraft.isVersionDraft(versionId)) {
            optLockValue = uiDraft.getOptLockValue();
        }

        structureController.deleteAttribute(uiDraft.getId(), optLockValue, attributeCode);
        return uiDraft;
    }

    public UiDraft updateDataRecord(Integer versionId, Integer optLockValue, Row row) {

        validatePresent(row);

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        // Изменение записи в опубликованной версии:
        if (!uiDraft.isVersionDraft(versionId)) {
            // Новый справочник, поэтому блокировки нет (см. также в других методах):
            optLockValue = uiDraft.getOptLockValue();
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
        List<Structure.Attribute> primaries = structure.getPrimaries();
        if (primaries.isEmpty())
            return;

        List<AttributeFilter> primaryFilters = RowUtils.getPrimaryKeyValueFilters(row, primaries);
        if (primaryFilters.isEmpty())
            return;

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setPageSize(1);
        criteria.addAttributeFilterList(primaryFilters);

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        if (rowValues != null && !isEmpty(rowValues.getContent())) {
            Message message = new Message(DATA_ROW_PK_EXISTS_EXCEPTION_CODE,
                    RowUtils.toNamedValues(row.getData(), primaries));
            throw new UserException(message);
        }
    }

    public UiDraft deleteDataRecord(Integer versionId, Integer optLockValue, Long id) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!uiDraft.isVersionDraft(versionId)) {
            optLockValue = uiDraft.getOptLockValue();
            id = findNewSystemId(id, versionId, uiDraft.getId());
        }

        Row row = new Row(id, emptyMap());
        DeleteDataRequest request = new DeleteDataRequest(optLockValue, row);
        draftService.deleteData(uiDraft.getId(), request);
        return uiDraft;
    }

    public UiDraft deleteAllDataRecords(Integer versionId, Integer optLockValue) {

        final UiDraft uiDraft = getOrCreateDraft(versionId);

        if (!uiDraft.isVersionDraft(versionId)) {
            optLockValue = uiDraft.getOptLockValue();
        }

        DeleteAllDataRequest request = new DeleteAllDataRequest(optLockValue);
        draftService.deleteAllData(uiDraft.getId(), request);
        return uiDraft;
    }

    /** Поиск идентификатора записи в черновике по старому идентификатору в текущей версии. */
    private Long findNewSystemId(Long oldSystemId, Integer oldVersionId, Integer newVersionId) {

        if (oldSystemId == null) return null;

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setRowSystemIds(singletonList(oldSystemId));

        Page<RefBookRowValue> oldRow = versionService.search(oldVersionId, criteria);
        if (isEmpty(oldRow.getContent()))
            throw new NotFoundException(UPDATED_DATA_NOT_FOUND_IN_CURRENT_EXCEPTION_CODE);

        SearchDataCriteria hashCriteria = new SearchDataCriteria();
        String hash = oldRow.getContent().get(0).getHash();
        hashCriteria.setRowHashList(singletonList(hash));

        final Page<RefBookRowValue> newRow = versionService.search(newVersionId, hashCriteria);
        if (isEmpty(newRow.getContent()))
            throw new NotFoundException(UPDATED_DATA_NOT_FOUND_IN_DRAFT_EXCEPTION_CODE);

        return newRow.getContent().get(0).getSystemId();
    }

    public UiDraft createFromFile(FileModel fileModel) {

        Integer versionId = refBookService.create(fileModel).getId();
        RefBookVersion version = versionService.getById(versionId);

        return new UiDraft(version);
    }

    public UiDraft uploadFromFile(Integer versionId, FileModel fileModel) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));

        if (!version.isDraft())
            throw new UserException(new Message(VERSION_IS_NOT_DRAFT_EXCEPTION_CODE, versionId));

        Draft draft = draftService.create(version.getRefBookId(), fileModel);
        return new UiDraft(draft, version.getRefBookId());
    }

    public UiDraft uploadData(Integer versionId, FileModel fileModel, Integer optLockValue) {

        RefBookVersion version = versionService.getById(versionId);
        if (version == null)
            throw new UserException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));

        if (!version.isDraft())
            throw new UserException(new Message(VERSION_IS_NOT_DRAFT_EXCEPTION_CODE, versionId));

        if (version.hasEmptyStructure())
            throw new UserException(new Message(VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE, versionId));

        UpdateFromFileRequest request = new UpdateFromFileRequest(optLockValue, fileModel);
        draftService.updateFromFile(versionId, request);

        return new UiDraft(version);
    }
}
