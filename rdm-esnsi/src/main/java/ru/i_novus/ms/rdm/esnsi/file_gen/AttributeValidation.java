package ru.i_novus.ms.rdm.esnsi.file_gen;

import ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType;

public class AttributeValidation {

    private final AttributeValidationType type;
    private final String value;

    public AttributeValidation(AttributeValidationType type, String value) {
        this.type = type;
        this.value = value;
    }

    public AttributeValidationType type() {
        return type;
    }

    public String value() {
        return value;
    }

}
