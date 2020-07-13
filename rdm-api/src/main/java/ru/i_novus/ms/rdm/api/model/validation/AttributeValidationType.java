package ru.i_novus.ms.rdm.api.model.validation;


import ru.i_novus.ms.rdm.api.exception.RdmException;

import java.lang.reflect.InvocationTargetException;

public enum AttributeValidationType {

    REQUIRED(RequiredAttributeValidation.class),
    UNIQUE(UniqueAttributeValidation.class),
    PLAIN_SIZE(PlainSizeAttributeValidation.class),
    FLOAT_SIZE(FloatSizeAttributeValidation.class),
    INT_RANGE(IntRangeAttributeValidation.class),
    FLOAT_RANGE(FloatRangeAttributeValidation.class),
    DATE_RANGE(DateRangeAttributeValidation.class),
    REG_EXP(RegExpAttributeValidation.class);

    private final Class<? extends AttributeValidation> validationValueClass;

    AttributeValidationType(Class<? extends AttributeValidation> validationValueClass) {
        this.validationValueClass = validationValueClass;
    }

    public AttributeValidation getValidationInstance() {
        try {
            return validationValueClass.getConstructor().newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RdmException(e);
        }
    }
}
