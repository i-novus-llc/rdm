package ru.inovus.ms.rdm.model.validation;

public class UniqueValidationValue extends AttributeValidationValue {

    public UniqueValidationValue() {
        super(AttributeValidationType.UNIQUE);
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
