package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import org.apache.commons.collections4.MapUtils;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Created by znurgaliev on 14.08.2018.
 */
public class DBPrimaryKeyValidation extends AppendRowValidation {

    private static final String DB_CONTAINS_PK_ERROR_CODE = "validation.db.contains.pk.err";

    private SearchDataService searchDataService;
    private List<Map<Structure.Attribute, Object>> primaryKeys;
    private List<String> primaryKeyAttributes;
    private String storageCode;
    private Collection<RowValue> rowValues;

    DBPrimaryKeyValidation(SearchDataService searchDataService, Structure structure, Row row, String storageCode) {
        this(searchDataService, structure, singletonList(row), storageCode);
    }

    public DBPrimaryKeyValidation(SearchDataService searchDataService, Structure structure, List<Row> rows, String storageCode) {
        this.searchDataService = searchDataService;
        this.storageCode = storageCode;
        this.primaryKeys = rows.stream().map(row -> getPrimaryKeyMap(structure, row)).filter(MapUtils::isNotEmpty).collect(toList());
        this.primaryKeyAttributes = isNotEmpty(primaryKeys)
                ? primaryKeys.get(0).keySet().stream().map(Structure.Attribute::getCode).collect(toList())
                : emptyList();
        this.rowValues = isNotEmpty(primaryKeys)
                ? getRefBookData(rows)
                : emptyList();
    }

    @Override
    public void appendRow(Row row) {
        setErrorAttributes(emptySet());
        super.appendRow(row);
    }

    @Override
    protected List<Message> validate(Long systemId, Map<String, Object> attributeValues) {
        if (!isEmpty(primaryKeyAttributes) &&
                primaryKeyAttributes.stream().noneMatch(a -> getErrorAttributes().contains(a))) {

            RowValue refBookRow = findRowValue(primaryKeyAttributes, attributeValues, rowValues);
            if (refBookRow != null && !refBookRow.getSystemId().equals(systemId)) {
                primaryKeyAttributes.forEach(this::addErrorAttribute);
                return singletonList(toMessageFromValues(attributeValues));
            }
        }
        return emptyList();
    }

    private Collection<RowValue> getRefBookData(List<Row> rows) {
        DataCriteria criteria = createCriteria(rows);
        return searchDataService.getPagedData(criteria).getCollection();
    }

    private DataCriteria createCriteria(List<Row> rows) {
        List<Field> fields = primaryKeys.get(0).keySet().stream().map(ConverterUtil::field).collect(toList());
        Set<List<FieldSearchCriteria>> filters = primaryKeys.stream()
                .filter(this::isCorrectType)
                .map(entry -> entry.entrySet().stream()
                        .map(this::toFieldSearchCriteria)
                        .collect(toList())
                ).collect(toSet());

        DataCriteria criteria = new DataCriteria(storageCode, null, null, fields, filters, null);
        criteria.setPage(1);
        int systemIdsCount = (int) rows.stream().map(Row::getSystemId).filter(Objects::nonNull).count();
        if (systemIdsCount > 0) {
            criteria.setSize(2 * systemIdsCount);
        } else {
            criteria.setSize(rows.size());
        }
        return criteria;
    }

    private static Map<Structure.Attribute, Object> getPrimaryKeyMap(Structure structure, Row row) {
        Map<Structure.Attribute, Object> map = new HashMap<>();
        structure.getPrimary()
                .forEach(attribute -> map.put(attribute, row.getData().get(attribute.getCode())));
        return map;
    }

    private boolean isCorrectType(Map<Structure.Attribute, Object> primaryAttributeValues) {
        return primaryAttributeValues.keySet().stream()
                .allMatch(primaryKeyAttr ->
                        TypeValidation.checkType(primaryKeyAttr.getType(), primaryKeyAttr.getCode(),
                                primaryAttributeValues.get(primaryKeyAttr)) == null
                );
    }

    private FieldSearchCriteria toFieldSearchCriteria(Map.Entry<Structure.Attribute, Object> primaryAttributeValue) {
        return new FieldSearchCriteria(
                ConverterUtil.field(primaryAttributeValue.getKey()),
                SearchTypeEnum.EXACT,
                singletonList(ConverterUtil.toSearchType(primaryAttributeValue.getValue())));
    }

    private Message toMessageFromValues(Map<String, Object> attributeValues) {
        return new Message(DB_CONTAINS_PK_ERROR_CODE, primaryKeys.get(0).keySet().stream()
                .map(primaryKey ->
                        primaryKey.getName() + "\" - \"" + attributeValues.get(primaryKey.getCode())
                ).collect(Collectors.joining("\", \"")));
    }

    /**
     * В списке записей #rowValues ищется строка, которая соответствует строке с данными #attributeValues
     * на основании набора значений первичных атрибутов #primaries.
     *
     * @param primaries список кодов первичных атрибутов со значениями для идентификации записи
     * @param attributeValues значения атрибутов строки, для которой ведется поиск
     * @param rowValues список записей, среди которых ведется поиск
     * @return Найденная запись либо null
     */
    private RowValue findRowValue(List<String> primaries,
                                         Map<String, Object> attributeValues,
                                         Collection<? extends RowValue> rowValues) {
        return rowValues
                .stream()
                .filter(rowValue ->
                        primaries.stream().allMatch(primary -> {
                            Object primaryValue = attributeValues.get(primary);
                            FieldValue fieldValue = rowValue.getFieldValue(primary);
                            return primaryValue != null
                                    && fieldValue != null
                                    && primaryValue.equals(fieldValue.getValue());
                        })
                )
                .findFirst()
                .orElse(null);
    }

}
