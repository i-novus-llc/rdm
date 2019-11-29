package ru.inovus.ms.rdm.impl.util;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RowDiffUtils {

    private RowDiffUtils() {
    }

    public static RowDiff getRowDiff(RowValue oldRowValue, Map<String, Object> newRow) {

        RowDiff rowDiff = new RowDiff();
        @SuppressWarnings("unchecked")
        List<FieldValue> fieldValues = oldRowValue.getFieldValues();

        fieldValues.stream()
                .filter(fieldValue ->
                        !Objects.equals(fieldValue.getValue(), newRow.get(fieldValue.getField())))
                .forEach(fieldValue -> {
                    Object oldValue = fieldValue.getValue();
                    Object newValue = newRow.get(fieldValue.getField());
                    RowDiff.CellDiff<Object> cellDiff = RowDiff.CellDiff.of(oldValue, newValue);
                    rowDiff.addDiff(fieldValue.getField(), cellDiff);
                });

        return rowDiff;
    }

    public static RowDiff getRowDiff(RowValue oldRowValue, RowValue newRowValue) {

        RowDiff rowDiff = new RowDiff();
        @SuppressWarnings("unchecked")
        List<FieldValue> fieldValues = oldRowValue.getFieldValues();

        fieldValues.stream()
                .filter(fieldValue -> !equalsFieldValue(fieldValue, newRowValue))
                .forEach(fieldValue ->
                        rowDiff.addDiff(fieldValue.getField(), getCellDiff(fieldValue, newRowValue))
                );
        return rowDiff;
    }

    private static boolean equalsFieldValue(FieldValue oldFieldValue, RowValue newRowValue) {

        FieldValue newFieldValue = newRowValue.getFieldValue(oldFieldValue.getField());
        return Objects.equals(oldFieldValue.getValue(), newFieldValue);
    }

    private static RowDiff.CellDiff<Object> getCellDiff(FieldValue oldFieldValue, RowValue newRowValue) {
        Object oldValue = oldFieldValue.getValue();
        Object newValue = newRowValue.getFieldValue(oldFieldValue.getField());
        return RowDiff.CellDiff.of(oldValue, newValue);
    }
}
