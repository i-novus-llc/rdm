package ru.inovus.ms.rdm.model;


public class ReadAttribute extends FormAttribute {

    // refBook
    private Integer versionId;

    // attribute
    private String codeExpression;

    // reference
    private Integer referenceRefBookId;
    private Integer displayType;
    private String displayAttribute;
    private String displayAttributeName;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getCodeExpression() {
        return codeExpression;
    }

    public void setCodeExpression(String codeExpression) {
        this.codeExpression = codeExpression;
    }

    public Integer getReferenceRefBookId() {
        return referenceRefBookId;
    }

    public void setReferenceRefBookId(Integer referenceRefBookId) {
        this.referenceRefBookId = referenceRefBookId;
    }

    public Integer getDisplayType() {
        return displayType;
    }

    public void setDisplayType(Integer displayType) {
        this.displayType = displayType;
    }

    public String getDisplayAttribute() {
        return displayAttribute;
    }

    public void setDisplayAttribute(String displayAttribute) {
        this.displayAttribute = displayAttribute;
    }

    public String getDisplayAttributeName() {
        return displayAttributeName;
    }

    public void setDisplayAttributeName(String displayAttributeName) {
        this.displayAttributeName = displayAttributeName;
    }
}
