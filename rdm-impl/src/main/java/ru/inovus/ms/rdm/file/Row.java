package ru.inovus.ms.rdm.file;

import java.util.LinkedHashMap;
import java.util.Map;

public class Row {

    private Map<String, Object> data = new LinkedHashMap<>();

    public Row(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Row row = (Row) o;

        return data != null ? data.equals(row.data) : row.data == null;
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }
}
