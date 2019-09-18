package ru.inovus.ms.rdm.n2o.model.refdata;

import java.util.Map;
import java.util.Objects;

public class Row {

    private Long systemId;

    private Map<String, Object> data;

    public Row() {}

    public Row(Map<String, Object> data) {
        this.data = data;
    }

    public Row(Long systemId, Map<String, Object> data) {
        this.systemId = systemId;
        this.data = data;
    }

    public Long getSystemId() {
        return systemId;
    }

    public void setSystemId(Long systemId) {
        this.systemId = systemId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
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
