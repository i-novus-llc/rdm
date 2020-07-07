package ru.inovus.ms.rdm.api.model.validation;

public class UniqueAttributeValidation extends AttributeValidation {

    public UniqueAttributeValidation() {
        super(AttributeValidationType.UNIQUE);
    }

    @Override
    public String valuesToString() {
        return null;
    }

    @Override
    public AttributeValidation valueFromString(String value) {
        return this;
    }
}
