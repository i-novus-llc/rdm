package ru.i_novus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.validation.FloatSizeAttributeValidation;

import java.math.BigDecimal;

/**
 * Проверка дины числа с точкой.
 * В бд value хранится в формате '(длина целой части);(длина дробной части)', например '5;2'
 */
public class FloatSizeAttributeValidationResolver implements AttributeValidationResolver<BigDecimal> {

    static final String FLOAT_INT_SIZE_EXCEPTION_CODE = "validation.float.int.size.err";
    static final String FLOAT_FRAC_SIZE_EXCEPTION_CODE = "validation.float.frac.size.err";

    private final Structure.Attribute attribute;
    private final int intPartSize;
    private final int fractionalPartSize;

    public FloatSizeAttributeValidationResolver(Structure.Attribute attribute, int intPartSize, int fractionalPartSize) {
        if (!FieldType.FLOAT.equals(attribute.getType()))
            throw new UserException("attribute.validation.type.invalid");
        this.attribute = attribute;
        this.intPartSize = intPartSize;
        this.fractionalPartSize = fractionalPartSize;
    }

    public FloatSizeAttributeValidationResolver(Structure.Attribute attribute, FloatSizeAttributeValidation validationValue) {
        this(attribute, validationValue.getIntPartSize(), validationValue.getFracPartSize());
    }

    @Override
    public Message resolve(BigDecimal bigDecimal) {
        if (bigDecimal != null) {
            if (bigDecimal.toBigInteger().toString().length() > intPartSize)
                return new Message(FLOAT_INT_SIZE_EXCEPTION_CODE, attribute.getName(), bigDecimal.toPlainString(), intPartSize);
            else if (bigDecimal.remainder(BigDecimal.ONE).toPlainString().length() - 2 > fractionalPartSize)
                return new Message(FLOAT_FRAC_SIZE_EXCEPTION_CODE, attribute.getName(), bigDecimal.toPlainString(), fractionalPartSize);
        }
        return null;
    }
}
