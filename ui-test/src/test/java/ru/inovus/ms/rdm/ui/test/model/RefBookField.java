package ru.inovus.ms.rdm.ui.test.model;

public class RefBookField {

    private final String code;
    private final String name;
    private final FieldType attributeTypeName;
    private final boolean isPrimaryKey;
    private final RefBook referredBook;

    public RefBookField(String code, String name, FieldType attributeTypeName, boolean isPrimaryKey, RefBook referredBook) {
        this.code = code;
        this.name = name;
        this.attributeTypeName = attributeTypeName;
        this.isPrimaryKey = isPrimaryKey;
        this.referredBook = referredBook;
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

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public RefBook getReferredBook() {
        return referredBook;
    }
}
