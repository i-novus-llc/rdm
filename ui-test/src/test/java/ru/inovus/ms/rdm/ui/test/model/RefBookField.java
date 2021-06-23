package ru.inovus.ms.rdm.ui.test.model;

import java.util.Map;

public class RefBookField {

    private final String code;
    private final String name;
    private final FieldType type;
    private final boolean isPrimary;

    private final RefBook referredBook;
    private final Map.Entry<Integer, String> referredField;

    public RefBookField(String code, String name, FieldType type, boolean isPrimary) {

        this.code = code;
        this.name = name;
        this.type = type;
        this.isPrimary = isPrimary;

        this.referredBook = null;
        this.referredField = null;
    }

    public RefBookField(String code, String name, FieldType type, boolean isPrimary,
                        RefBook referredBook, Map.Entry<Integer, String> referredField) {

        this.code = code;
        this.name = name;
        this.type = type;
        this.isPrimary = isPrimary;

        this.referredBook = referredBook;
        this.referredField = referredField;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public FieldType getType() {
        return type;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public RefBook getReferredBook() {
        return referredBook;
    }

    public Map.Entry<Integer, String> getReferredField() {
        return referredField;
    }

    public boolean isReferenceType() {
        return FieldType.REFERENCE.equals(getType());
    }
}
