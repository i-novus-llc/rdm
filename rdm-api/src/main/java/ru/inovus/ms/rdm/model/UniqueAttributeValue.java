package ru.inovus.ms.rdm.model;

public class UniqueAttributeValue {

    private Long systemId;

    private Object value;

    public UniqueAttributeValue(Long systemId, Object value) {
        this.systemId = systemId;
        this.value = value;
    }

    public Long getSystemId() {
        return systemId;
    }

    public void setSystemId(Long systemId) {
        this.systemId = systemId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}