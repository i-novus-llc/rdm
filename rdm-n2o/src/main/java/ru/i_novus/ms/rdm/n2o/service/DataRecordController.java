package ru.i_novus.ms.rdm.n2o.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.parseLocalDate;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

@Controller
@SuppressWarnings("unused") // used in: *RecordQueryResolver
public class DataRecordController {

    private static final Logger logger = LoggerFactory.getLogger(DataRecordController.class);

    private static final int MAX_FIXED_FIELD_COUNT = 5;

    @Autowired
    private VersionRestService versionService;

    @Autowired
    private DraftRestService draftService;

    /**
     * Получение записи версии справочника по параметрам.
     *
     * @param criteria критерий поиска
     */
    public Map<String, Serializable> getRow(DataRecordCriteria criteria) {

        String dataAction = criteria.getDataAction();
        if (StringUtils.isEmpty(dataAction))
            throw new IllegalArgumentException("A data action is not specified");

        RefBookVersion version = versionService.getById(criteria.getVersionId());

        Map<String, Serializable> map = createRowMap(version, criteria);

        switch (dataAction) {
            case DATA_ACTION_CREATE: return getCreatedRow(version, map);
            case DATA_ACTION_UPDATE: return getUpdatedRow(version, criteria.getId(), map);
            case "localize": return getUpdatedRow(version, criteria.getId(), map);
            default: return emptyMap();
        }
    }

    /** Создание набора для заполнения. */
    private Map<String, Serializable> createRowMap(RefBookVersion version, DataRecordCriteria criteria) {

        int atributeCount = version.getStructure().getAttributes().size();
        Map<String, Serializable> map = new HashMap<>(MAX_FIXED_FIELD_COUNT + atributeCount);

        map.put(FIELD_VERSION_ID, version.getId());
        map.put(FIELD_OPT_LOCK_VALUE, criteria.getOptLockValue());
        map.put(FIELD_LOCALE_CODE, criteria.getLocaleCode());
        map.put(FIELD_DATA_ACTION, criteria.getDataAction());

        return map;
    }

    /** Заполнение набора для создания записи. */
    private Map<String, Serializable> getCreatedRow(RefBookVersion version, Map<String, Serializable> map) {

        // id == null, поэтому не указывается.

        // Значения по умолчанию при создании записи заполнять здесь.

        // Get default values from backend by versionService.searchDefaults(versionId) instead of:
        version.getStructure().getReferences().forEach(reference ->
                map.put(addPrefix(reference.getAttribute()), new Reference())
        );

        return map;
    }

    /** Заполнение набора для изменения записи. */
    private Map<String, Serializable> getUpdatedRow(RefBookVersion version, Integer id,
                                                    Map<String, Serializable> map) {

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
