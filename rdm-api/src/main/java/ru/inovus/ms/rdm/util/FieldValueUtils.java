package ru.inovus.ms.rdm.util;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.compare.ComparableFieldValue;
import ru.inovus.ms.rdm.model.field.ReferenceFilterValue;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.version.AttributeFilter;

import java.math.BigInteger;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static ru.i_novus.platform.datastorage.temporal.model.DataConstants.*;

public class FieldValueUtils {

    private FieldValueUtils() {
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param rowValue          запись со значениями подставляемых полей
     * @return Отображаемое значение
     */
    public static String rowValueToDisplayValue(String displayExpression, RowValue rowValue) {
        return fieldValuesToDisplayValue(displayExpression, ((LongRowValue)rowValue).getFieldValues());
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param fieldValues       список значений подставляемых полей
     * @return Отображаемое значение
     */
    private static String fieldValuesToDisplayValue(String displayExpression, List<FieldValue> fieldValues) {
        Map<String, Object> map = new HashMap<>();
        fieldValues.forEach(fieldValue -> map.put(fieldValue.getField(), fieldValue.getValue()));
        return new StringSubstitutor(map, DisplayExpression.PLACEHOLDER_START, DisplayExpression.PLACEHOLDER_END).replace(displayExpression);
    }

    /**
     * Проверка на наличие хотя бы одного placeholder`а в выражении.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param placeholders      список проверяемых подставляемых значений
     * @return Наличие
     */
    // NB: Выделить в displayExpressionUtils ?!
    public static boolean containsAnyPlaceholder(String displayExpression, List<String> placeholders) {
        DisplayExpression expression = new DisplayExpression(displayExpression);
        return CollectionUtils.containsAny(expression.getPlaceholders(), placeholders);
    }

    /**
     * Поиск полей выражения, которые отсутствуют в структуре.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param structure         структура версии, на которую ссылаются
     * @return Список отсутствующих полей
     */
    public static List<String> getAbsentPlaceholders(String displayExpression, Structure structure) {

        if (StringUtils.isEmpty(displayExpression))
            return emptyList();

        DisplayExpression expression = new DisplayExpression(displayExpression);
        return expression.getPlaceholders().stream()
                .filter(placeholder -> Objects.isNull(structure.getAttribute(placeholder)))
                .collect(toList());
    }

    /**
     * Возвращает типизированное значение атрибута.
     *
     * @param fieldValue   значение атрибута
     * @param refFieldType тип атрибута, к которому приводится значение
     * @return Типизированное значение атрибута
     */
    public static Object castFieldValue(FieldValue fieldValue, FieldType refFieldType) {
        if (fieldValue instanceof ReferenceFieldValue) {
            return castRefValue((Reference) (fieldValue.getValue()), refFieldType);
        }
        return fieldValue.getValue();
    }

    /**
     * Возвращает типизированное значение ссылки.
     *
     * <p>При приведении типа используется тип атрибута, НА который ссылаемся.</p>
     *
     * @param value        ссылка
     * @param refFieldType тип атрибута, на который ссылаемся
     * @return Типизированное значение ссылочного атрибута
     */
    private static Object castRefValue(Reference value, FieldType refFieldType) {
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
     * Получение множества фильтров атрибута по системным идентификаторам.
     *
     * @param systemIds системные идентификаторы
     * @return Множество фильтров
     */
    public static Set<List<AttributeFilter>> toSystemIdFilters(List<Long> systemIds) {
        return systemIds.stream()
                .map(systemId -> new AttributeFilter(SYS_PRIMARY_COLUMN, BigInteger.valueOf(systemId), FieldType.INTEGER))
                .map(Collections::singletonList)
                .collect(toSet());
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
                    Object attributeValue = castFieldValue(value.getReferenceValue(), value.getAttribute().getType());
                    return new AttributeFilter(value.getAttribute().getCode(), attributeValue, value.getAttribute().getType(), SearchTypeEnum.EXACT);
                })
                .map(Collections::singletonList)
                .collect(toSet());
    }

    public static Object getDiffFieldValue(DiffFieldValue fieldValue, DiffStatusEnum status) {
        return DiffStatusEnum.DELETED.equals(status) ? fieldValue.getOldValue() : fieldValue.getNewValue();
    }

    @SuppressWarnings("WeakerAccess")
    public static Object getCompareFieldValue(ComparableFieldValue fieldValue, DiffStatusEnum status) {
        return DiffStatusEnum.DELETED.equals(status) ? fieldValue.getOldValue() : fieldValue.getNewValue();
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
        Map<String, Object> map = new HashMap<>();
        diffFieldValues.forEach(fieldValue ->
                map.put(fieldValue.getField().getName(), getDiffFieldValue(fieldValue, diffStatus))
        );
        return new StringSubstitutor(map, DisplayExpression.PLACEHOLDER_START, DisplayExpression.PLACEHOLDER_END).replace(displayExpression);
    }
}
