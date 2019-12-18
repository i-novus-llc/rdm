package ru.inovus.ms.rdm.api.model.refdata;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.api.model.Structure;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("squid:S1948")
public class Row implements Serializable {

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

    public boolean equalsByAttributes(RefBookRowValue refBookRowValue, List<Structure.Attribute> attributes) {
        for (Structure.Attribute attr : attributes) {
            Object v1 = this.getData().get(attr.getCode());
            FieldValue v2 = refBookRowValue.getFieldValue(attr.getCode());
            if ((v1 != null && v2 == null) || (v1 == null && v2 != null))
                return false;
            if (v1 == null)
                continue;
            if (v1 instanceof Reference && v2.getValue() instanceof Reference && !Objects.equals(((Reference) v1).getValue(), ((Reference) v2.getValue()).getValue()))
                return false;
            if  (!Objects.equals(v1, v2.getValue()))
                return false;
        }
        return true;
    }

}
