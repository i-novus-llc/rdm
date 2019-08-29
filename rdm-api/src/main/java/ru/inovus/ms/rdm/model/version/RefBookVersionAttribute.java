package ru.inovus.ms.rdm.model.version;

import ru.inovus.ms.rdm.model.Structure;

import java.io.Serializable;

public class RefBookVersionAttribute implements Serializable {

    private Integer versionId;

    private Structure.Attribute attribute;

    private Structure.Reference reference;

    @SuppressWarnings("WeakerAccess")
    public RefBookVersionAttribute() {}

    public RefBookVersionAttribute(Integer versionId, Structure.Attribute attribute, Structure.Reference reference) {
        this.versionId = versionId;
        this.attribute = attribute;
        this.reference = reference;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Structure.Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Structure.Attribute attribute) {
        this.attribute = attribute;
    }

    public Structure.Reference getReference() {
        return reference;
    }

    public void setReference(Structure.Reference reference) {
        this.reference = reference;
    }
}