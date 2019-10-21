package ru.inovus.ms.rdm.impl.util;

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

        private final T oldVal;
        private final T newVal;

        private CellDiff(T oldVal, T newVal) {
            this.oldVal = oldVal;
            this.newVal = newVal;
        }

        @JsonGetter
        public T getOldVal() {
            return oldVal;
        }

        @JsonGetter
        public T getNewVal() {
            return newVal;
        }

        public static <T> CellDiff<T> of(T oldVal, T newVal) {
            if (Objects.equals(oldVal, newVal))
                throw new IllegalArgumentException("Values are equal. Zero diff.");
            return new CellDiff<>(oldVal, newVal);
        }

    }

}
