package ru.i_novus.ms.rdm.api.model.version;

import java.io.Serializable;

public class UniqueAttributeValue {

    private Long systemId;

    private Serializable value;

    public UniqueAttributeValue(Long systemId, Serializable value) {
        this.systemId = systemId;
        this.value = value;
    }

    public Long getSystemId() {
        return systemId;
    }

    public void setSystemId(Long systemId) {
        this.systemId = systemId;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

}
