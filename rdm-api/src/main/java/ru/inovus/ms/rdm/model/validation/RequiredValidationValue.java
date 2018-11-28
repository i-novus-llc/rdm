package ru.inovus.ms.rdm.model.validation;

public class RequiredValidationValue extends AttributeValidationValue {

    public RequiredValidationValue() {
        super(AttributeValidationType.REQUIRED);
    }

    @Override
    public String valuesToString() {
        return null;
    }

    @Override
    public AttributeValidationValue valueFromString(String value) {
        return this;
    }
}
