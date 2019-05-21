package ru.inovus.ms.rdm.model;


public class ReadAttribute extends FormAttribute {

    private Integer versionId;

    private String codeExpression;

    private Integer referenceRefBookId;

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
}
