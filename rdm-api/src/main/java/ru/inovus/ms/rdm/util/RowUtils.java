package ru.inovus.ms.rdm.util;

import org.apache.commons.text.StringSubstitutor;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RowUtils {

    private RowUtils() {
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param rowValue          запись со значениями подставляемых полей
     * @return Отображаемое значение
     */
    public static String toDisplayValue(String displayExpression, RowValue rowValue) {
        return toDisplayValue(displayExpression, ((LongRowValue)rowValue).getFieldValues());
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param fieldValues       список значений подставляемых полей
     * @return Отображаемое значение
     */
    private static String toDisplayValue(String displayExpression, List<FieldValue> fieldValues) {
        Map<String, Object> map = new HashMap<>();
        fieldValues.forEach(fieldValue -> map.put(fieldValue.getField(), fieldValue.getValue()));
        return new StringSubstitutor(map, DisplayExpression.PLACEHOLDER_START, DisplayExpression.PLACEHOLDER_END).replace(displayExpression);
    }

    /**
     * Получение отображаемого значения.
     *
     * @param displayExpression выражение для вычисления отображаемого значения
     * @param diffFieldValues   список отличий значений подставляемых полей
     * @param diffStatus        статус отличия значения
     * @return Отображаемое значение
     */
    public static String toDisplayValue(String displayExpression, List<DiffFieldValue> diffFieldValues, DiffStatusEnum diffStatus) {
        Map<String, Object> map = new HashMap<>();
        diffFieldValues.forEach(fieldValue ->
            map.put(fieldValue.getField().getName(), fieldValue.getValue(diffStatus))
        );
        return new StringSubstitutor(map, DisplayExpression.PLACEHOLDER_START, DisplayExpression.PLACEHOLDER_END).replace(displayExpression);
    }
}
