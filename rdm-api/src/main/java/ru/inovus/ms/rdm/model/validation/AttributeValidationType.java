package ru.inovus.ms.rdm.model.validation;


import ru.inovus.ms.rdm.exception.RdmException;

import java.lang.reflect.InvocationTargetException;

public enum AttributeValidationType {

    REQUIRED(RequiredValidationValue.class),
    UNIQUE(UniqueValidationValue.class),
    PLAIN_SIZE(PlainSizeValidationValue.class),
    FLOAT_SIZE(FloatSizeValidationValue.class),
    INT_RANGE(IntRangeValidationValue.class),
    FLOAT_RANGE(FloatRangeValidationValue.class),
    DATE_RANGE(DateRangeValidationValue.class),
    REG_EXP(RegExpValidationValue.class);

    private final Class<? extends AttributeValidationValue> validationValueClass;

    AttributeValidationType(Class<? extends AttributeValidationValue> validationValueClass) {
        this.validationValueClass = validationValueClass;
    }

    public AttributeValidationValue getValidationInstance() {
        try {
            return validationValueClass.getConstructor().newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RdmException(e);
        }
    }
}
