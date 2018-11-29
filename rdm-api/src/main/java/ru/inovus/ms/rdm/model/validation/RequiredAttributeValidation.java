package ru.inovus.ms.rdm.model.validation;

public class RequiredAttributeValidation extends AttributeValidation {

    public RequiredAttributeValidation() {
        super(AttributeValidationType.REQUIRED);
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
