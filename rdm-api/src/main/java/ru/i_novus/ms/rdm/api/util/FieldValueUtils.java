package ru.i_novus.ms.rdm.api.util;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.exception.RdmException;
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

public class FieldValueUtils {

    private static final String PRIMARY_KEY_CODE_DELIMITER = ": ";

    private FieldValueUtils() {
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
                map.put(fieldValue.getField(), toDisplayValue(fieldValue, placeholders))
        );

        List<String> absentPlaceholders = placeholders.keySet().stream()
                        .filter(placeholder -> Objects.isNull(map.get(placeholder)))
                        .collect(toList());
        absentPlaceholders.forEach(absent -> map.put(absent, ""));

        String displayValue = createDisplayExpressionSubstitutor(map).replace(displayExpression);

        if (!CollectionUtils.containsAny(placeholders.keySet(), primaryKeyCodes)) {

            String primaryKeysValue = primaryKeyCodes.stream()
                    .map(code -> String.valueOf(map.get(code)))
                    .filter(value -> !StringUtils.isEmpty(value))
                    .reduce("", (result, value) -> result + value + PRIMARY_KEY_CODE_DELIMITER);
            displayValue = primaryKeysValue + displayValue;
        }

        return displayValue;
    }

    /** Получение отображаемого значения из поля. */
    private static String toDisplayValue(FieldValue fieldValue, Map<String, String> placeholders) {

        if (fieldValue.getValue() != null)
            return String.valueOf(fieldValue.getValue());

        String value = placeholders.get(fieldValue.getField());
        return (value != null) ? value : "";
    }

    /**
     * Получение типизированного значения атрибута.
     *
     * @param fieldValue   значение атрибута
     * @param refFieldType тип атрибута, к которому приводится значение
     * @return Типизированное значение атрибута
     */
    public static Serializable castFieldValue(FieldValue fieldValue, FieldType refFieldType) {
        if (fieldValue instanceof ReferenceFieldValue) {
            return castRefValue((Reference) (fieldValue.getValue()), refFieldType);
        }
        return fieldValue.getValue();
    }

    /**
     * Получение типизированного значения ссылки.
     *
     * <p>При приведении типа используется тип атрибута, НА который ссылаемся.</p>
     *
     * @param value        ссылка
     * @param refFieldType тип атрибута, на который ссылаемся
     * @return Типизированное значение ссылочного атрибута
     */
    private static Serializable castRefValue(Reference value, FieldType refFieldType) {
        if (refFieldType == FieldType.INTEGER) {
            return value.getValue() != null ? new BigInteger(value.getValue()) : null;
        }
        return value.getValue();
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
     * Получение множества фильтров атрибута по ссылочным значениям.
     *
     * @param filterValues ссылочные значения
     * @return Множество фильтров
     */
    public static Set<List<AttributeFilter>> toAttributeFilters(List<ReferenceFilterValue> filterValues) {
        return filterValues.stream()
                .map(value -> {
                    Serializable attributeValue = castFieldValue(value.getReferenceValue(), value.getAttribute().getType());
                    return new AttributeFilter(value.getAttribute().getCode(), attributeValue, value.getAttribute().getType(), SearchTypeEnum.EXACT);
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

    public static FieldValue getFieldValueFromFieldType(Object value, String fieldCode, FieldType fieldType) {
        switch (fieldType) {
            case STRING:
                return new StringFieldValue(fieldCode, (String) value);
            case INTEGER: return new IntegerFieldValue(fieldCode, (BigInteger) value);
            case REFERENCE: return new ReferenceFieldValue(fieldCode, (Reference) value);
            case FLOAT: return new FloatFieldValue(fieldCode, (BigDecimal) value);
            case BOOLEAN: return new BooleanFieldValue(fieldCode, (Boolean) value);
            case DATE: return new DateFieldValue(fieldCode, (LocalDate) value);
            case TREE: return new TreeFieldValue(fieldCode, (String) value);
            default: throw new RdmException("Unexpected field type: " + fieldType);
        }
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param diffFieldValues   список отличий значений подставляемых полей
     * @param diffStatus        статус отличия значения
     * @return Отображаемое значение
     */
    public static String diffValuesToDisplayValue(String displayExpression, List<DiffFieldValue> diffFieldValues, DiffStatusEnum diffStatus) {

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
