package ru.inovus.ms.rdm.model;


import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

public class Attribute {

    private String code;

    private String name;

    private FieldType type;

    private Boolean isPrimary;

    private Boolean isRequired;

    private String description;

    private Integer referenceVersion;

    private String referenceAttribute;

    private String referenceDisplayExpression;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public Boolean getIsPrimary() {
        return isPrimary != null && isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Boolean getIsRequired() {
        return isRequired != null && isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(Integer referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public String getReferenceAttribute() {
        return referenceAttribute;
    }

    public void setReferenceAttribute(String referenceAttribute) {
        this.referenceAttribute = referenceAttribute;
    }

    public String getReferenceDisplayExpression() {
        return referenceDisplayExpression;
    }

    public void setReferenceDisplayExpression(String referenceDisplayExpression) {
        this.referenceDisplayExpression = referenceDisplayExpression;
    }
}
