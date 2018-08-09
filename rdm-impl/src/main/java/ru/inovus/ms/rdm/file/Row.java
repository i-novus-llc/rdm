package ru.inovus.ms.rdm.file;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
        return Objects.equals(data, row.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
