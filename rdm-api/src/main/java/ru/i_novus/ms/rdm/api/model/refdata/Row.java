package ru.i_novus.ms.rdm.api.model.refdata;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/** Строка - запись справочника. */
public class Row implements Serializable {

    /** Системный идентификатор записи. */
    private Long systemId;

    /** Набор значений полей записи. */
    private Map<String, Object> data; // NOSONAR

    public Row() {
        // Nothing to do.
    }

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
