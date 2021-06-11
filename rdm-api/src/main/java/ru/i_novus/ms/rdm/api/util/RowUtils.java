package ru.i_novus.ms.rdm.api.util;

import org.springframework.util.ObjectUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
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

    /** Получение списка systemIds из коллекции записей. */
    @SuppressWarnings("unchecked")
    public static <T> List<T> toSystemIds(Collection<? extends RowValue> rowValues) {
        return rowValues.stream().map(rowValue -> (T) rowValue.getSystemId()).collect(toList());
    }

    /** Преобразование списка systemIds из vds в список для rdm. */
    public static List<Long> toLongSystemIds(Collection<Object> systemIds) {
        return systemIds.stream().map(systemId -> (Long) systemId).collect(toList());
    }

    /**
     * Проверка на системного идентификатора записи с указанным системным идентификатором.
     *
     * @param rowValue запись справочника
     * @param systemId системный идентификатор
     * @return Результат проверки
     */
    public static boolean hasSystemId(RowValue<?> rowValue, Object systemId) {

        return systemId.equals(rowValue.getSystemId());
    }

    /**
     * Получение записи из коллекции по указанному системному идентификатору.
     *
     * @param rowValues коллекция записей справочника
     * @param systemId  системный идентификатор
     * @return Запись справочника
     */
    public static RowValue getBySystemId(Collection<RowValue> rowValues, Object systemId) {

        return rowValues.stream()
                .filter(rowValue -> hasSystemId(rowValue, systemId))
                .findFirst().orElse(null);
    }

    /**
     * Проверка на наличие записи с указанным системным идентификатором в коллекции.
     *
     * @param rowValues коллекция записей справочника
     * @param systemId  системный идентификатор
     * @return Результат проверки
     */
    public static boolean containsSystemId(Collection<RowValue> rowValues, Object systemId) {

        return !isEmpty(rowValues)
                && rowValues.stream().anyMatch(rowValue -> hasSystemId(rowValue, systemId));
    }

    /**
     * Преобразование значения первичного ключа записи в значение для поиска.
     *
     * @param primary  первичный ключ
     * @param rowValue запись справочника
     * @return Значение для поиска
     */
    public static Serializable toSearchValue(Structure.Attribute primary, RowValue rowValue) {

        FieldValue fieldValue = rowValue.getFieldValue(primary.getCode());
        return FieldValueUtils.castFieldValue(fieldValue, primary.getType());
    }

    /**
     * Преобразование значения первичных ключей записей в строковые значения ссылки на эти записи.
     *
     * @param primaries список первичных ключей
     * @param rowValues записи справочника
     * @return Строковые значения ссылки
     */
    public static List<String> toReferenceValues(List<Structure.Attribute> primaries, Collection<RowValue> rowValues) {

        return rowValues.stream().map(rowValue -> toReferenceValue(primaries, rowValue)).collect(toList());
    }

    /**
     * Преобразование значения первичных ключей записи в строковое значение ссылки на эту запись.
     *
     * @param primaries список первичных ключей
     * @param rowValue  запись справочника
     * @return Строковое значение ссылки
     */
    public static String toReferenceValue(List<Structure.Attribute> primaries, RowValue rowValue) {

        // На данный момент первичным ключом может быть только одно поле.
        // Ссылка на значение составного ключа невозможна.
        FieldValue fieldValue = rowValue.getFieldValue(primaries.get(0).getCode());
        Serializable value = FieldValueUtils.castFieldValue(fieldValue, FieldType.STRING);

        return value != null ? value.toString() : null;
    }

    /**
     * Преобразование записей в набор с привязкой к строковым значениям ссылки.
     *
     * @param primaries список первичных ключей
     * @param rowValues записи справочника
     * @return Набор записей
     */
    public static Map<String, RowValue> toReferredRowValues(List<Structure.Attribute> primaries,
                                                            Collection<RowValue> rowValues) {
        return rowValues.stream()
                .collect(toMap(rowValue -> toReferenceValue(primaries, rowValue), Function.identity()));
    }

    /**
     * Получение ссылки из указанного поля в записи с заданным системным идентификатором.
     *
     * @param rowValues список записей
     * @param systemId  системный идентификатор
     * @param fieldCode наименование поля-ссылки = код атрибута-ссылки
     * @return Ссылка или null
     */
    public static Reference getFieldReference(Collection<? extends RowValue> rowValues,
                                              Long systemId, String fieldCode) {

        RowValue foundRowValue = rowValues.stream()
                .filter(rowValue -> Objects.equals(rowValue.getSystemId(), systemId))
                .findFirst().orElse(null);
        if (foundRowValue == null)
            return null;

        FieldValue fieldValue = foundRowValue.getFieldValue(fieldCode);
        return (fieldValue instanceof ReferenceFieldValue) ? ((ReferenceFieldValue) fieldValue).getValue() : null;
    }

    /**
     * Получение значения ссылки из указанного поля-ссылки в записи.
     *
     * @param rowValue  запись ссылочного справочника
     * @param fieldCode наименование поля-ссылки = код атрибута-ссылки
     * @return Значение поля-ссылки или null
     */
    public static String getFieldReferenceValue(RowValue rowValue, String fieldCode) {

        Serializable value = rowValue.getFieldValue(fieldCode).getValue();
        return value != null ? ((Reference) value).getValue() : null;
    }

    /**
     * Получение фильтров по точному совпадению значений первичных ключей из записи.
     *
     * @param row       запись со значениями
     * @param primaries первичные ключи
     * @return Список фильтров
     */
    public static List<AttributeFilter> toPrimaryKeyValueFilters(Row row, List<Structure.Attribute> primaries) {

        return primaries.stream()
                .map(primary -> toPrimaryKeyFilter(row, primary))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private static AttributeFilter toPrimaryKeyFilter(Row row, Structure.Attribute primary) {

        Serializable value = (Serializable) row.getData().get(primary.getCode());
        if (value == null)
            return null;

        if (value instanceof Reference) {
            value = ((Reference) value).getValue();
        }

        return new AttributeFilter(primary.getCode(), value, primary.getType(), SearchTypeEnum.EXACT);
    }
}
