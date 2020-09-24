package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.n2o.criteria.DataRecordCriteria;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.parseLocalDate;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil.addPrefix;

@Controller
@SuppressWarnings("unused") // used in: *RecordQueryResolver
public class DataRecordController {

    private static final Logger logger = LoggerFactory.getLogger(DataRecordController.class);

    private static final String CONFLICT_TEXT = "conflict.text";
    private static final String CONFLICT_TEXT_UPDATED = "conflict.text.updated";
    private static final String CONFLICT_TEXT_DELETED = "conflict.text.deleted";
    private static final String CONFLICT_TEXT_ALTERED = "conflict.text.altered";

    private static final int MAX_FIXED_FIELD_COUNT = 5;

    @Autowired
    private VersionRestService versionService;

    @Autowired
    private DraftRestService draftService;

    @Autowired
    private ConflictService conflictService;

    @Autowired
    private Messages messages;

    /**
     * Получение записи версии справочника по параметрам.
     *
     * @param criteria критерий поиска
     */
    public Map<String, Object> getRow(DataRecordCriteria criteria) {

        String dataAction = criteria.getDataAction();
        if (StringUtils.isEmpty(dataAction))
            throw new IllegalArgumentException("A data action is not specified");

        RefBookVersion version = versionService.getById(criteria.getVersionId());

        Map<String, Object> map = createRowMap(version, criteria);

        switch (dataAction) {
            case DATA_ACTION_CREATE: return getCreatedRow(version, map);
            case DATA_ACTION_UPDATE: return getUpdatedRow(version, criteria.getId(), map);
            case "localize": return getUpdatedRow(version, criteria.getId(), map);
            default: return emptyMap();
        }
    }

    /** Создание набора для заполнения. */
    private Map<String, Object> createRowMap(RefBookVersion version, DataRecordCriteria criteria) {

        int atributeCount = version.getStructure().getAttributes().size();
        Map<String, Object> map = new HashMap<>(MAX_FIXED_FIELD_COUNT + atributeCount);

        map.put(FIELD_VERSION_ID, version.getId());
        map.put(FIELD_OPT_LOCK_VALUE, criteria.getOptLockValue());
        map.put(FIELD_LOCALE_CODE, criteria.getLocaleCode());
        map.put(FIELD_DATA_ACTION, criteria.getDataAction());

        return map;
    }

    /** Заполнение набора для создания записи. */
    private Map<String, Object> getCreatedRow(RefBookVersion version, Map<String, Object> map) {

        // id == null, поэтому не указывается.

        // Значения по умолчанию при создании записи заполнять здесь.

        // Get default values from backend by versionService.searchDefaults(versionId) instead of:
        version.getStructure().getReferences().forEach(reference ->
                map.put(addPrefix(reference.getAttribute()), new Reference())
        );

        return map;
    }

    /** Заполнение набора для изменения записи. */
    private Map<String, Object> getUpdatedRow(RefBookVersion version, Integer id, Map<String, Object> map) {

        map.put(FIELD_SYSTEM_ID, id);

        List<RefBookRowValue> rowValues = findRowValues(version.getId(), id);
        if (isEmpty(rowValues))
            return emptyMap();

        rowValues.get(0).getFieldValues().forEach(
                fieldValue -> map.put(addPrefix(fieldValue.getField()), fieldValue.getValue())
        );

        return map;
    }

    /**
     * Получение записи из указанной версии справочника по системному идентификатору.
     */
    private List<RefBookRowValue> findRowValues(Integer versionId, Integer id) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setRowSystemIds(singletonList(id.longValue()));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return !isEmpty(rowValues.getContent()) ? rowValues.getContent() : emptyList();
    }

    /**
     * Проверка наличия конфликтов для записи у ссылающейся версии.
     *
     * @param versionId идентификатор версии, которая ссылается
     * @param id        идентификатор записи этой версии
     * @return Строка со всеми конфликтами
     */
    public String getDataConflicts(Integer versionId, Long id) {

        final Structure structure = getStructureOrNull(versionId);
        if (structure == null || isEmpty(structure.getReferences()))
            return null;

        List<String> refFieldCodes = StructureUtils.getReferenceAttributeCodes(structure).collect(toList());
        List<RefBookConflict> conflicts = findDataConflicts(versionId, id, refFieldCodes);
        if (isEmpty(conflicts))
            return null;

        return conflicts.stream()
                .map(conflict -> getConflictText(conflict.getConflictType(),
                        () -> getConflictRefFieldName(conflict, structure)))
                .collect(joining(" \n"));
    }

    private Structure getStructureOrNull(Integer versionId) {
        try {
            return versionService.getStructure(versionId);

        } catch (Exception e) {
            logger.error("Structure is not received for data", e);

            return null;
        }
    }

    /**
     * Поиск конфликта по ссылающейся версии, идентификатору строки и ссылкам.
     *
     * @param versionId     идентификатор версии, которая ссылается
     * @param id            идентификатор записи этой версии
     * @param refFieldCodes список кодов ссылок в структуре этой версии
     * @return Список конфликтов
     */
    private List<RefBookConflict> findDataConflicts(Integer versionId,
                                                    Long id,
                                                    List<String> refFieldCodes) {
        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(versionId);
        criteria.setIsLastPublishedVersion(true);
        criteria.setRefFieldCodes(refFieldCodes);
        criteria.setRefRecordId(id);
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
     * @param optLockValue значение оптимистической блокировки версии-черновика
     */
    @SuppressWarnings("WeakerAccess")
    public void updateData(Integer draftId, Row row, Integer optLockValue) {
        row.getData().entrySet().stream()
                .filter(e -> e.getValue() instanceof Date)
                .forEach(e -> e.setValue(parseLocalDate(e.getValue())));

        UpdateDataRequest request = new UpdateDataRequest(optLockValue, singletonList(row));
        draftService.updateData(draftId, request);
    }
}
