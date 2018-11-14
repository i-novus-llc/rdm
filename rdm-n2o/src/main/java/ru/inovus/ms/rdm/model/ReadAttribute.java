package ru.inovus.ms.rdm.model;


public class ReadAttribute extends Attribute {

    private Integer versionId;

    private Integer referenceRefBookId;

    private String referenceAttributeName;

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

    public String getReferenceAttributeName() {
        return referenceAttributeName;
    }

    public void setReferenceAttributeName(String referenceAttributeName) {
        this.referenceAttributeName = referenceAttributeName;
    }
}
