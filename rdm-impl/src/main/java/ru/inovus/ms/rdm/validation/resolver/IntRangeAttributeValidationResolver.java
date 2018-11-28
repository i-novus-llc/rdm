package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.IntRangeValidationValue;

import java.math.BigInteger;

/**
 * Проверка вхождения integer в диапазон.
 * В бд value хранится в формате '(минимальное значение);(максимальное значение)', например '-999;999'
 */
public class IntRangeAttributeValidationResolver implements AttributeValidationResolver<BigInteger> {

    public static final String INT_RANGE_EXCEPTION_CODE = "validation.range.err";

    private final Structure.Attribute attribute;
    private final BigInteger min;
    private final BigInteger max;

    public IntRangeAttributeValidationResolver(Structure.Attribute attribute, BigInteger min, BigInteger max) {
        if (!FieldType.INTEGER.equals(attribute.getType()))
            throw new UserException("attribute.validation.type.invalid");
        this.attribute = attribute;
        this.min = min;
        this.max = max;
    }

    public IntRangeAttributeValidationResolver(Structure.Attribute attribute, IntRangeValidationValue validationValue) {
        this(attribute, validationValue.getMin(), validationValue.getMax());
    }

    @Override
    public Message resolve(BigInteger value) {
        if (value == null) return null;
        boolean isLargerThanMin = min == null || value.compareTo(min) >= 0;
        boolean isLessThanMax = max == null || value.compareTo(max) <= 0;
        return isLargerThanMin && isLessThanMax ? null : new Message(INT_RANGE_EXCEPTION_CODE, attribute, value);
    }
}
