package ru.inovus.ms.rdm.api.model.refdata;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import java.util.Map;
import java.util.Objects;

public class Row {

    private Long systemId;

    /**
     * Типы данных, в зависимости от {@link FieldType типа}, указанного в {@link ru.inovus.ms.rdm.api.model.Structure структуре} поля :
     * {@link FieldType#STRING} -> String
     * {@link FieldType#INTEGER} -> BigInteger
     * {@link FieldType#FLOAT} -> Double
     * {@link FieldType#REFERENCE} -> String
     * {@link FieldType#DATE} -> String (в формате, который может понять метод {@link TimeUtils#parseLocalDate(Object)}
     * {@link FieldType#BOOLEAN} -> Boolean
     * {@link FieldType#TREE} -> ????
     */
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
