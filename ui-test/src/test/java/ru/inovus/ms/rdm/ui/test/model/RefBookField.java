package ru.inovus.ms.rdm.ui.test.model;

import ru.inovus.ms.rdm.ui.test.login.FieldType;

public class RefBookField {

    private final String code;
    private final String name;
    private final FieldType attributeTypeName;

    public RefBookField(String code, String name, FieldType attributeTypeName) {
        this.code = code;
        this.name = name;
        this.attributeTypeName = attributeTypeName;
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
