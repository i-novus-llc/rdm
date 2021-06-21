package ru.i_novus.ms.rdm.api.util;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableFieldValue;
import ru.i_novus.ms.rdm.api.model.field.ReferenceFilterValue;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.DATE_PATTERN_ERA_FORMATTER;

@SuppressWarnings({"rawtypes", "java:S3740"})
public class FieldValueUtils {

    private static final String PRIMARY_KEY_VALUE_DISPLAY_DELIMITER = ": ";

    private FieldValueUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param rowValue          запись со значениями подставляемых полей
     * @param primaryKeyCodes   список кодов первичных ключей
     * @return Отображаемое значение
     */
    public static String toDisplayValue(String displayExpression,
                                        RowValue rowValue,
                                        List<String> primaryKeyCodes) {
        return toDisplayValue(displayExpression, ((LongRowValue)rowValue).getFieldValues(), primaryKeyCodes);
    }

    /**
     * Получение отображаемого значения.
     * <p/>
     * Подставляет в выражение отображаемого значения в соответствии с подстановками в нём
     * значения полей из списка или значения по умолчанию из подстановок.
     * <p/>
     * При наличии кодов первичных ключей позволяет добавить к полученной строке
     * префикс из значений этих ключей, если выражение не содержит хотя бы один первичный ключ,
     * для обеспечения уникальности получаемого результата при обработке списка таких строк.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param fieldValues       список значений подставляемых полей
     * @param primaryKeyCodes   список кодов первичных ключей
     * @return Отображаемое значение
     */
    private static String toDisplayValue(String displayExpression,
                                         List<FieldValue> fieldValues,
                                         List<String> primaryKeyCodes) {

        Map<String, String> placeholders = new DisplayExpression(displayExpression).getPlaceholders();

        Map<String, Object> map = new HashMap<>();
        fieldValues.forEach(fieldValue ->
                map.put(fieldValue.getField(), toPlaceholderValue(fieldValue, placeholders))
        );

        List<String> absentPlaceholders = placeholders.keySet().stream()
                .filter(placeholder -> Objects.isNull(map.get(placeholder)))
                .collect(toList());
        absentPlaceholders.forEach(absent -> map.put(absent, ""));

        String displayValue = createDisplayExpressionSubstitutor(map).replace(displayExpression);

        if (!CollectionUtils.isEmpty(primaryKeyCodes) &&
                !CollectionUtils.containsAny(placeholders.keySet(), primaryKeyCodes)) {

            String primaryKeysValue = primaryKeyCodes.stream()
                    .map(code -> String.valueOf(map.get(code)))
                    .filter(value -> !StringUtils.isEmpty(value))
                    .reduce("", (result, value) -> result + value + PRIMARY_KEY_VALUE_DISPLAY_DELIMITER);
            displayValue = primaryKeysValue + displayValue;
        }

        return displayValue;
    }

    /**
     * Получение значения для подстановки в отображаемое значение
     * из значения поля при его наличии или из значения по умолчанию из подстановки.
     *
     * @param fieldValue   значение из поля
     * @param placeholders список подстановок со значениями по умолчанию
     * @return Значение для подстановки в отображаемое значение
     */
    private static String toPlaceholderValue(FieldValue fieldValue, Map<String, String> placeholders) {

        if (fieldValue.getValue() != null)
            return String.valueOf(fieldValue.getValue());

        String value = placeholders.get(fieldValue.getField());
        return (value != null) ? value : "";
    }

    /**
     * Приведение значения поля ссылки к значению указанного типа.
     *
     * @param fieldValue  значение поля
     * @param toFieldType тип атрибута, к которому приводится значение
     * @return Типизированное значение атрибута
     */
    public static Serializable castFieldValue(FieldValue fieldValue, FieldType toFieldType) {

        if (fieldValue instanceof ReferenceFieldValue) {
            return castReferenceFieldValue((Reference) (fieldValue.getValue()), toFieldType);
        }

        return fieldValue.getValue();
    }

    /**
     * Приведение значения из ссылки к значению указанного типа.
     *
     * <p>При приведении значения ссылки к значению первичного ключа
     * используется тип атрибута, НА который ссылаемся.</p>
     *
     * @param value       ссылка
     * @param toFieldType тип, к которому приводится значение
     * @return Типизированное значение ссылки
     */
    private static Serializable castReferenceFieldValue(Reference value, FieldType toFieldType) {

        return (value.getValue() == null || toFieldType == null)
                ? null
                : castReferenceValue(value.getValue(), toFieldType);
    }

    /**
     * Приведение строкового значения ссылки к значению указанного типа.
     *
     * @param value       строковое значение ссылки
     * @param toFieldType тип, к которому приводится значение
     * @return Типизированное значение ссылки
     */
    public static Serializable castReferenceValue(String value, FieldType toFieldType) {

        return switch (toFieldType) {
            case INTEGER -> new BigInteger(value);
            case FLOAT -> Float.parseFloat(value);
            case DATE -> LocalDate.parse(value, DATE_PATTERN_ERA_FORMATTER);
            case BOOLEAN -> Boolean.valueOf(value);
            default -> value;
        };
    }

    /**
     * Получение значений первичных ключей
     * по записи {@code rowValue} на основании структуры {@code structure}.
     *
     * @param rowValue  запись справочника
     * @param structure структура справочника
     * @return Список значений полей для первичных ключей
     */
    public static List<FieldValue> getRowPrimaryValues(RefBookRowValue rowValue, Structure structure) {

        if (rowValue == null || structure == null)
            return emptyList();

        return rowValue.getFieldValues().stream()
                .filter(fieldValue ->
                        structure.getAttribute(fieldValue.getField()).getIsPrimary())
                .collect(toList());
    }

    /**
     * Проверка на наличие записи со значением поля.
     *
     * @param field     код поля
     * @param value     значение поля
     * @param rowValues список записей
     * @return Наличие строки
     */
    public static boolean isFieldValueRow(String field, Object value, List<RefBookRowValue> rowValues) {

        return rowValues.stream()
                .anyMatch(rowValue -> Objects.equals(rowValue.getFieldValue(field).getValue(), value));
    }

    /**
     * Получение набора фильтров по атрибуту по ссылочным значениям.
     *
     * @param filterValues ссылочные значения
     * @return Набор фильтров по атрибуту
     */
    public static Set<List<AttributeFilter>> toAttributeFilters(List<ReferenceFilterValue> filterValues) {

        return filterValues.stream()
                .map(value -> {
                    FieldType attributeType = value.getAttribute().getType();
                    Serializable attributeValue = castFieldValue(value.getReferenceValue(), attributeType);
                    return new AttributeFilter(value.getAttribute().getCode(), attributeValue, attributeType, SearchTypeEnum.EXACT);
                })
                .map(Collections::singletonList)
                .collect(toSet());
    }

    public static Serializable getDiffFieldValue(DiffFieldValue fieldValue, DiffStatusEnum status) {

        return DiffStatusEnum.DELETED.equals(status)
                ? (Serializable) fieldValue.getOldValue()
                : (Serializable) fieldValue.getNewValue();
    }

    @SuppressWarnings("WeakerAccess")
    public static Object getCompareFieldValue(ComparableFieldValue fieldValue, DiffStatusEnum status) {

        return DiffStatusEnum.DELETED.equals(status) ? fieldValue.getOldValue() : fieldValue.getNewValue();
    }

    public static FieldValue toFieldValueByType(Object value, String fieldCode, FieldType fieldType) {

        return switch (fieldType) {
            case STRING -> new StringFieldValue(fieldCode, (String) value);
            case INTEGER -> new IntegerFieldValue(fieldCode, (BigInteger) value);
            case REFERENCE -> new ReferenceFieldValue(fieldCode, (Reference) value);
            case FLOAT -> new FloatFieldValue(fieldCode, (BigDecimal) value);
            case BOOLEAN -> new BooleanFieldValue(fieldCode, (Boolean) value);
            case DATE -> new DateFieldValue(fieldCode, (LocalDate) value);
            case TREE -> new TreeFieldValue(fieldCode, (String) value);
        };
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param diffFieldValues   список отличий значений подставляемых полей
     * @param diffStatus        статус отличия значения
     * @return Отображаемое значение
     */
    public static String diffValuesToDisplayValue(String displayExpression,
                                                  List<DiffFieldValue> diffFieldValues, DiffStatusEnum diffStatus) {

        Map<String, Object> map = new HashMap<>(diffFieldValues.size());
        diffFieldValues.forEach(fieldValue ->
                map.put(fieldValue.getField().getName(), getDiffFieldValue(fieldValue, diffStatus))
        );
        return createDisplayExpressionSubstitutor(map).replace(displayExpression);
    }

    /** Создание объекта подстановки в выражение для вычисления отображаемого значения. */
    public static StringSubstitutor createDisplayExpressionSubstitutor(Map<String, Object> map) {

        StringSubstitutor substitutor = new StringSubstitutor(map,
                DisplayExpression.PLACEHOLDER_START, DisplayExpression.PLACEHOLDER_END);
        substitutor.setValueDelimiter(DisplayExpression.PLACEHOLDER_DEFAULT_DELIMITER);

        return substitutor;
    }
}
