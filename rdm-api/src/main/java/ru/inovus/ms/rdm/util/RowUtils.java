package ru.inovus.ms.rdm.util;

import org.apache.commons.text.StringSubstitutor;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.HashMap;
import java.util.Map;

public class RowUtils {

    private RowUtils() {
    }

    public static String toDisplayValue(String displayExpression, RowValue rowValue) {
        Map<String, Object> map = new HashMap<>();
        ((LongRowValue)rowValue).getFieldValues().forEach(fieldValue -> map.put(fieldValue.getField(), fieldValue.getValue()));
        return new StringSubstitutor(map).replace(displayExpression);
    }
}
