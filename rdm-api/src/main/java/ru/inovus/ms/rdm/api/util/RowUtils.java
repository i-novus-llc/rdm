package ru.inovus.ms.rdm.api.util;

import org.springframework.util.ObjectUtils;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@SuppressWarnings("rawtypes")
public class RowUtils {

    private RowUtils() {
    }

    /** Проверка строки данных на наличие значений. */
    public static boolean isEmptyRow(Row row) {
        return row == null
                || row.getData().values().stream().allMatch(ObjectUtils::isEmpty);
    }

    /** Сравнение значения из Row::getData::get и из RefBookRowValue::getFieldValue. */
    public static boolean equalsValues(Object newDataValue, FieldValue<?> oldFieldValue) {

        if ((newDataValue != null && oldFieldValue == null)
                || (newDataValue == null && oldFieldValue != null))
            return false;

        if (newDataValue == null)
            return true;

        Object oldDataValue = oldFieldValue.getValue();
        if (newDataValue instanceof Reference
                && oldDataValue instanceof Reference
                && !Objects.equals(((Reference) newDataValue).getValue(),  ((Reference) oldDataValue).getValue()))
            return false;

        return Objects.equals(newDataValue, oldDataValue);
    }

    /** Сравнение значений из Row и из RefBookRowValue по атрибутам. */
    public static boolean equalsValuesByAttributes(Row newRow, RefBookRowValue oldRowValue, List<Structure.Attribute> attributes) {
        return attributes.stream()
                .allMatch(attribute -> {
                    Object newDataValue = newRow.getData().get(attribute.getCode());
                    @SuppressWarnings("unchecked")
                    FieldValue<Serializable> oldFieldValue = oldRowValue.getFieldValue(attribute.getCode());
                    return RowUtils.equalsValues(newDataValue, oldFieldValue);
                });
    }

    /** Преобразование списка systemIds из vds в список для rdm. */
    public static List<Long> toLongSystemIds(List<Object> systemIds) {
        return systemIds.stream().map(systemId -> (Long) systemId).collect(toList());
    }

    /** Проверка на совпадение systemId со значением из RowValue. */
    public static boolean isSystemIdRowValue(Object systemId, RowValue<?> rowValue) {
        return systemId.equals(rowValue.getSystemId());
    }

    /** Получение значения RowValue с совпадающим значением systemId. */
    public static RowValue getSystemIdRowValue(Object systemId, List<RowValue> rowValues) {
        return rowValues.stream()
                .filter(rowValue -> isSystemIdRowValue(systemId, rowValue))
                .findFirst().orElse(null);
    }

    /** Проверка на наличие значения RowValue с совпадающим значением systemId. */
    public static boolean existsSystemIdRowValue(Object systemId, List<RowValue> rowValues) {
        return !isEmpty(rowValues)
                && rowValues.stream().anyMatch(rowValue -> isSystemIdRowValue(systemId, rowValue));
    }

    public static List<AttributeFilter> getPrimaryKeyValueFilters(Row row, List<Structure.Attribute> primaryKeys) {
        return primaryKeys.stream()
                .map(key -> {
                    Object value = row.getData().get(key.getCode());
                    if (value == null)
                        return null;

                    if (value instanceof Reference)
                        value = ((Reference) value).getValue();

                    return new AttributeFilter(key.getCode(), value, key.getType(), SearchTypeEnum.EXACT);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
