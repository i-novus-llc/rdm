package ru.inovus.ms.rdm.model;


public class ReadAttribute extends FormAttribute {

    private Integer versionId;

    private Integer referenceRefBookId;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getReferenceRefBookId() {
        return referenceRefBookId;
    }

    public void setReferenceRefBookId(Integer referenceRefBookId) {
        this.referenceRefBookId = referenceRefBookId;
    }
}
