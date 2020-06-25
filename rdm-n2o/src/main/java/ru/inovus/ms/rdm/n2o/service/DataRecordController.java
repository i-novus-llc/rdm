package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.service.ConflictService;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.n2o.provider.DataRecordConstants;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.platform.datastorage.temporal.model.DataConstants.SYS_PRIMARY_COLUMN;
import static ru.inovus.ms.rdm.api.util.TimeUtils.parseLocalDate;
import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.addPrefix;

@Controller
@SuppressWarnings("unused")
public class DataRecordController {

    private static final String ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE = "action.draft.was.changed";

    private static final String CONFLICT_TEXT = "conflict.text";
    private static final String CONFLICT_TEXT_UPDATED = "conflict.text.updated";
    private static final String CONFLICT_TEXT_DELETED = "conflict.text.deleted";
    private static final String CONFLICT_TEXT_ALTERED = "conflict.text.altered";

    @Autowired
    Messages messages;

    @Autowired
    private VersionService versionService;

    @Autowired
    private DraftService draftService;

    @Autowired
    private ConflictService conflictService;

    /**
     * Получение записи версии справочника для создания/редактирования.
     *
     * @param versionId    идентификатор версии справочника
     * @param sysRecordId  идентификатор записи
     * @param optLockValue значение оптимистической блокировки
     * @param dataAction   действие, которое планируется выполнять над записью
     */
    public Map<String, Object> getRow(Integer versionId, Integer sysRecordId, Integer optLockValue, String dataAction) {

        RefBookVersion version = versionService.getById(versionId);
        if (optLockValue != null && !optLockValue.equals(version.getOptLockValue()))
            throw new UserException(new Message(ACTION_DRAFT_WAS_CHANGED_EXCEPTION_CODE));

        if (StringUtils.isEmpty(dataAction))
            throw new IllegalArgumentException("data action is not supported");

        Map<String, Object> map = createRow(version, optLockValue, dataAction);

        switch (dataAction) {
            case DataRecordConstants.DATA_ACTION_CREATE: return getCreatedRow(version, map);
            case DataRecordConstants.DATA_ACTION_EDIT: return getUpdatedRow(versionId, sysRecordId, map);
            default: return emptyMap();
        }
    }

    private Map<String, Object> createRow(RefBookVersion version, Integer optLockValue, String dataAction) {

        int atributeCount = version.getStructure().getAttributes().size();
        Map<String, Object> map = new HashMap<>(4 + atributeCount);

        map.put(DataRecordConstants.FIELD_VERSION_ID, version.getId());
        map.put(DataRecordConstants.FIELD_OPT_LOCK_VALUE, optLockValue);
        map.put(DataRecordConstants.FIELD_DATA_ACTION, dataAction);

        return map;
    }

    private Map<String, Object> getCreatedRow(RefBookVersion version, Map<String, Object> map) {

        // sysRecordId is null

        // Get default values from backend by versionService.searchDefaults(versionId) instead of:
        version.getStructure().getReferences().forEach(reference ->
                map.put(addPrefix(reference.getAttribute()), new Reference())
        );

        return map;
    }

    private Map<String, Object> getUpdatedRow(Integer versionId, Integer sysRecordId, Map<String, Object> map) {

        map.put(DataRecordConstants.FIELD_SYSTEM_ID, sysRecordId);

        List<RefBookRowValue> rowValues = findRowValues(versionId, sysRecordId);
        if (isEmpty(rowValues))
            return emptyMap();

        rowValues.get(0).getFieldValues().forEach(
                fieldValue -> map.put(addPrefix(fieldValue.getField()), fieldValue.getValue())
        );

        return map;
    }

    private List<RefBookRowValue> findRowValues(Integer versionId, Integer sysRecordId) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordIdFilter = new AttributeFilter(SYS_PRIMARY_COLUMN, sysRecordId, FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordIdFilter)));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return !isEmpty(rowValues.getContent()) ? rowValues.getContent() : emptyList();
    }

    /**
     * Проверка наличия конфликтов для записи у ссылаемой версии.
     *
     * @param referrerVersionId идентификатор версии, которая ссылается
     * @param sysRecordId       идентификатор записи этой версии
     * @return Строка со всеми конфликтами
     */
    public String getDataConflicts(Integer referrerVersionId, Long sysRecordId) {

        final Structure structure = versionService.getStructure(referrerVersionId);
        if (isEmpty(structure.getReferences()))
            return null;

        List<String> refFieldCodes = structure.getReferences().stream()
                .map(Structure.Reference::getAttribute)
                .collect(toList());
        List<RefBookConflict> conflicts = findDataConflicts(referrerVersionId, sysRecordId, refFieldCodes);
        if (isEmpty(conflicts))
            return null;

        return conflicts.stream()
                .map(conflict -> getConflictText(conflict.getConflictType(),
                        () -> getConflictRefFieldName(conflict, structure)))
                .collect(joining(" \n"));
    }

    /**
     * Поиск конфликта по ссылаемой версии, идентификатору строки и ссылкам.
     *
     * @param referrerVersionId идентификатор версии, которая ссылается
     * @param rowSystemId       идентификатор записи этой версии
     * @param refFieldCodes     список кодов ссылок в структуре этой версии
     * @return Список конфликтов
     */
    private List<RefBookConflict> findDataConflicts(Integer referrerVersionId, Long rowSystemId,
                                                    List<String> refFieldCodes) {
        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(referrerVersionId);
        criteria.setIsLastPublishedVersion(true);
        criteria.setRefFieldCodes(refFieldCodes);
        criteria.setRefRecordId(rowSystemId);
        criteria.setPageSize(refFieldCodes.size());

        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return (conflicts != null) ? conflicts.getContent() : emptyList();
    }

    /** Получение названия атрибута с конфликтом. */
    private String getConflictRefFieldName(RefBookConflict conflict, Structure structure) {

        Structure.Attribute attribute = structure.getAttribute(conflict.getRefFieldCode());
        return attribute != null ? attribute.getName() : null;
    }

    /** Получение описания конфликта для атрибута. */
    private String getConflictText(ConflictType type, Supplier<String> attributeName) {

        String typeText = getConflictTypeText(type);
        if (typeText == null)
            return null;

        return messages.getMessage(CONFLICT_TEXT, attributeName.get(), messages.getMessage(typeText));
    }

    /** Получение описания конфликта по его типу. */
    private String getConflictTypeText(ConflictType type) {
        switch (type) {
            case UPDATED: return CONFLICT_TEXT_UPDATED;
            case DELETED: return CONFLICT_TEXT_DELETED;
            case ALTERED: return CONFLICT_TEXT_ALTERED;
            default: return null;
        }
    }

    /**
     * Обновление строки данных версии справочника.
     *
     * @param draftId      идентификатор черновика справочника
     * @param row          строка данных для добавления/изменения
     * @param optLockValue значение оптимистической блокировки
     */
    @SuppressWarnings("WeakerAccess")
    public void updateData(Integer draftId, Row row, Integer optLockValue) {
        row.getData().entrySet().stream()
                .filter(e -> e.getValue() instanceof Date)
                .forEach(e -> e.setValue(parseLocalDate(e.getValue())));

        draftService.updateData(draftId, row);
    }
}
