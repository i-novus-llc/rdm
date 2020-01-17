package ru.inovus.ms.rdm.api.model.refdata;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class Row implements Serializable {

    private Long systemId;
    private Map<String, Object> data; // NOSONAR

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

        Row that = (Row) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
