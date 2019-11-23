package ru.inovus.ms.rdm.impl.util;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RowDiffUtils {

    private RowDiffUtils() {
    }

    public static RowDiff getRowDiff(RowValue oldRow, Map<String, Object> newRow) {

        RowDiff rowDiff = new RowDiff();
        @SuppressWarnings("unchecked")
        List<FieldValue> fieldValues = oldRow.getFieldValues();

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
}
