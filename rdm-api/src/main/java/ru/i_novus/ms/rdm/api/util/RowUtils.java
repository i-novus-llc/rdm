package ru.i_novus.ms.rdm.api.util;

import org.springframework.util.ObjectUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.parseLocalDate;

@SuppressWarnings({"rawtypes", "java:S3740"})
public class RowUtils {

    private RowUtils() {
        // Nothing to do.
    }

    /** Проверка набора данных на отсутствие значений. */
    public static boolean isEmptyData(final Map< ?, ? > map) {

        return map == null || map.isEmpty();
    }

    /** Проверка plain-записи данных на отсутствие значений. */
    public static boolean isEmptyRow(Row row) {

        return row == null || isEmptyData(row.getData())
                || row.getData().values().stream().allMatch(ObjectUtils::isEmpty);
    }

    /** Подготовка значений plain-записи к выполнению операции над записью. */
    public static void prepareRowValues(Row row) {

        if (isEmptyRow(row))
            return;

        row.getData().entrySet().stream()
                .filter(e -> e.getValue() instanceof Date)
                .forEach(e -> e.setValue(parseLocalDate(e.getValue())));
    }

    /**
     * Сравнение значения из разных записей.
     *
     * @param newDataValue  значение из Row::getData::get
     * @param oldFieldValue значение из RefBookRowValue::getFieldValue
     * @return Признак успешности проверки
     */
    public static boolean equalsValues(Object newDataValue, FieldValue<?> oldFieldValue) {

        if ((newDataValue == null) != (oldFieldValue == null))
            return false;

        if (newDataValue == null)
            return true;

        Object oldDataValue = oldFieldValue.getValue();
        if (newDataValue instanceof Reference
                && oldDataValue instanceof Reference
                && !Objects.equals(((Reference) newDataValue).getValue(), ((Reference) oldDataValue).getValue()))
            return false;

        return Objects.equals(newDataValue, oldDataValue);
    }

    /**
     * Сравнение значений по атрибутам.
     *
     * @param newRow      новая запись
     * @param oldRowValue старая запись
     * @param attributes  список атрибутов
     * @return Признак успешности проверки
     */
    public static boolean equalsValuesByAttributes(Row newRow, RefBookRowValue oldRowValue,
                                                   List<Structure.Attribute> attributes) {
        return attributes.stream()
                .allMatch(attribute -> {
                    Object newDataValue = newRow.getData().get(attribute.getCode());
                    @SuppressWarnings("unchecked")
                    FieldValue<Serializable> oldFieldValue = oldRowValue.getFieldValue(attribute.getCode());
                    return equalsValues(newDataValue, oldFieldValue);
                });
    }

    /**
     * Преобразование значений атрибутов с их наименованиями в строку.
     *
     * @param rowData    запись
     * @param attributes список атрибутов
     * @return Результат преобразования
     */
    public static String toNamedValues(Map<String, Object> rowData,
                                       List<Structure.Attribute> attributes) {
        return attributes.stream()
                .map(attribute -> toNamedValue(rowData, attribute))
                .collect(Collectors.joining("\", \""));
    }

    private static String toNamedValue(Map<String, Object> rowData, Structure.Attribute attribute) {
        return attribute.getName() + "\" - \"" + rowData.get(attribute.getCode());
    }

    public static List<Object> toSystemIds(List<RowValue> rowValues) {
        return rowValues.stream().map(RowValue::getSystemId).collect(toList());
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
    public static boolean isSystemIdRowValue(Object systemId, List<RowValue> rowValues) {
        return !isEmpty(rowValues)
                && rowValues.stream().anyMatch(rowValue -> isSystemIdRowValue(systemId, rowValue));
    }

    /**
     * Создание фильтров по точному совпадению значений первичных ключей.
     *
     * @param row       запись со значениями
     * @param primaries первичные ключи
     * @return Список фильтров
     */
    public static List<AttributeFilter> getPrimaryKeyValueFilters(Row row, List<Structure.Attribute> primaries) {
        return primaries.stream()
                .map(key -> {
                    Serializable value = (Serializable) row.getData().get(key.getCode());
                    if (value == null)
                        return null;

                    if (value instanceof Reference) {
                        value = ((Reference) value).getValue();
                    }

                    return new AttributeFilter(key.getCode(), value, key.getType(), SearchTypeEnum.EXACT);
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }
}
