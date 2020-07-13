package ru.i_novus.ms.rdm.impl.util;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Разница строк в пределах одной версии.
 */
public class RowDiff {

    private final Map<String, CellDiff> cellDiff = new HashMap<>();

    public <T> void addDiff(String field, CellDiff<T> diff) {
        cellDiff.put(field, diff);
    }

    @JsonGetter
    public Map<String, CellDiff> getDiff() {
        return cellDiff;
    }

    public static class CellDiff<T> {

        private final T oldValue;
        private final T newValue;

        private CellDiff(T oldValue, T newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @JsonGetter
        public T getOldValue() {
            return oldValue;
        }

        @JsonGetter
        public T getNewValue() {
            return newValue;
        }

        public static <T> CellDiff<T> of(T oldValue, T newValue) {

            if (Objects.equals(oldValue, newValue))
                return null;

            return new CellDiff<>(oldValue, newValue);
        }
    }
}
