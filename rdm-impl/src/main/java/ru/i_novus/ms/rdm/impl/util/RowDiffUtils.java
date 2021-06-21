package ru.i_novus.ms.rdm.impl.util;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

@SuppressWarnings({"rawtypes","java:S3740"})
public class RowDiffUtils {

    private RowDiffUtils() {
    }

    public static RowDiff getRowDiff(RowValue oldRowValue, RowValue newRowValue) {

        RowDiff rowDiff = new RowDiff();
        @SuppressWarnings("unchecked")
        List<FieldValue> fieldValues = oldRowValue.getFieldValues();
        for (FieldValue fieldValue : fieldValues) {
            RowDiff.CellDiff<Object> cellDiff = getCellDiff(fieldValue, newRowValue);
            if (cellDiff != null) { // Только несовпадающие:
                rowDiff.addDiff(fieldValue.getField(), cellDiff);
            }
        }
        return rowDiff;
    }

    private static RowDiff.CellDiff<Object> getCellDiff(FieldValue oldFieldValue, RowValue newRowValue) {

        Object oldValue = oldFieldValue.getValue();
        FieldValue newValue = newRowValue.getFieldValue(oldFieldValue.getField());
        return RowDiff.CellDiff.of(oldValue, newValue != null ? newValue.getValue() : null);
    }
}
