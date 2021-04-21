package ru.inovus.ms.rdm.ui.test.model;

public class RefBookField {

    private final String code;
    private final String name;
    private final FieldType attributeTypeName;
    private boolean isPrimaryKey;

    public RefBookField(String code, String name, FieldType attributeTypeName) {
        this.code = code;
        this.name = name;
        this.attributeTypeName = attributeTypeName;
    }

    public Boolean getPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(Boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public FieldType getAttributeTypeName() {
        return attributeTypeName;
    }
}
