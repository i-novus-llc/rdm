package ru.i_novus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import org.apache.commons.collections4.MapUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * Проверка на уникальность строк по первичным ключам в таблице БД.
 */
public class DBPrimaryKeyValidation extends AppendRowValidation {

    private static final String DB_CONTAINS_PK_ERROR_CODE = "validation.db.contains.pk.err";

    private final SearchDataService searchDataService;
    private final String storageCode;

    private final List<Structure.Attribute> primaryKeys;
    private final List<String> primaryKeyCodes;

    private final List<Map<Structure.Attribute, Serializable>> primaryKeyMaps;
    private final List<RowValue> primarySearchValues;

    public DBPrimaryKeyValidation(SearchDataService searchDataService, String storageCode, Structure structure, Row row) {
        this(searchDataService, storageCode, structure, singletonList(row));
    }

    public DBPrimaryKeyValidation(SearchDataService searchDataService, String storageCode, Structure structure, List<Row> rows) {

        this.searchDataService = searchDataService;
        this.storageCode = storageCode;

        this.primaryKeys = structure.getPrimaries();
        this.primaryKeyCodes = primaryKeys.stream().map(Structure.Attribute::getCode).collect(toList());

        this.primaryKeyMaps = toPrimaryKeyMaps(rows);
        this.primarySearchValues = getRefBookData(rows);
    }

    @Override
    public void appendRow(Row row) {
        setErrorAttributes(emptySet());
        super.appendRow(row);
    }

    @Override
    protected List<Message> validate(Long systemId, Map<String, Object> rowData) {

        if (!isEmpty(primaryKeyCodes) &&
                primaryKeyCodes.stream().noneMatch(this::isErrorAttribute)) {

            RowValue rowValue = findRowValue(primaryKeyCodes, rowData, primarySearchValues);
            if (rowValue != null && !rowValue.getSystemId().equals(systemId)) {
                primaryKeyCodes.forEach(this::addErrorAttribute);
                return singletonList(createMessage(rowData));
            }
        }
        return emptyList();
    }

    private List<Map<Structure.Attribute, Serializable>> toPrimaryKeyMaps(List<Row> rows) {
        return rows.stream().map(this::toPrimaryKeyMap).filter(MapUtils::isNotEmpty).collect(toList());
    }

    private Map<Structure.Attribute, Serializable> toPrimaryKeyMap(Row row) {
        Map<Structure.Attribute, Serializable> map = new HashMap<>();
        primaryKeys.forEach(attribute -> map.put(attribute, (Serializable) row.getData().get(attribute.getCode())));
        return map;
    }

    private List<RowValue> getRefBookData(List<Row> rows) {

        if (isEmpty(primaryKeyMaps))
            return emptyList();

        StorageDataCriteria criteria = createCriteria(rows);
        Collection<RowValue> rowValues = searchDataService.getPagedData(criteria).getCollection();
        return !isEmpty(rowValues) ? rowValues.stream().map(this::toPrimaryRowValue).collect(toList()) : emptyList();
    }

    private StorageDataCriteria createCriteria(List<Row> rows) {

        List<Field> fields = primaryKeys.stream().map(ConverterUtil::field).collect(toList());
        Set<List<FieldSearchCriteria>> fieldFilters = primaryKeyMaps.stream()
                .filter(this::isCorrectType)
                .map(entry -> entry.entrySet().stream()
                        .map(this::toFieldSearchCriteria)
                        .collect(toList())
                ).collect(toSet());

        StorageDataCriteria criteria = new StorageDataCriteria(storageCode, null, null, fields, fieldFilters, null);
        criteria.setPage(BaseDataCriteria.MIN_PAGE);
        criteria.setSize(calculateCriteriaSize(rows));
        return criteria;
    }

    private int calculateCriteriaSize(List<Row> rows) {

        int systemIdsCount = (int) rows.stream().map(Row::getSystemId).filter(Objects::nonNull).count();
        return (systemIdsCount > 0) ? 2 * systemIdsCount : rows.size();
    }

    private boolean isCorrectType(Map<Structure.Attribute, Serializable> primaryKeyMap) {

        return primaryKeyMap.keySet().stream()
                .allMatch(attribute ->
                        TypeValidation.validateType(attribute, primaryKeyMap.get(attribute)) == null
                );
    }

    private FieldSearchCriteria toFieldSearchCriteria(Map.Entry<Structure.Attribute, Serializable> primaryKeyValue) {
        return new FieldSearchCriteria(
                ConverterUtil.field(primaryKeyValue.getKey()),
                SearchTypeEnum.EXACT,
                singletonList(ConverterUtil.toSearchValue(primaryKeyValue.getValue())));
    }

    private RowValue toPrimaryRowValue(RowValue rowValue) {

        List<FieldValue> fieldValues = primaryKeyCodes.stream()
                .map(rowValue::getFieldValue)
                .filter(Objects::nonNull)
                .collect(toList());
        rowValue.setFieldValues(fieldValues);

        return rowValue;
    }

    private Message createMessage(Map<String, Object> rowData) {
        return new Message(DB_CONTAINS_PK_ERROR_CODE, RowUtils.toNamedValues(rowData, primaryKeys));
    }

    /**
     * В списке записей #searchValues ищется запись, которая соответствует строке с данными #rowData
     * на основании набора значений первичных атрибутов #primaries.
     *
     * @param primaries    список кодов первичных атрибутов со значениями для идентификации записи
     * @param rowData      строка с данными, для которой ведётся поиск
     * @param searchValues список записей, среди которых ведётся поиск
     * @return Найденная запись либо null
     */
    private RowValue findRowValue(List<String> primaries,
                                  Map<String, Object> rowData,
                                  List<RowValue> searchValues) {
        return searchValues.stream()
                .filter(searchValue ->
                        primaries.stream().allMatch(primary -> {
                            Object primaryValue = rowData.get(primary);
                            FieldValue fieldValue = searchValue.getFieldValue(primary);
                            return primaryValue != null
                                    && fieldValue != null
                                    && primaryValue.equals(fieldValue.getValue());
                        })
                )
                .findFirst().orElse(null);
    }
}
