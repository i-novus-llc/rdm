package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.model.validation.FloatRangeAttributeValidation;

import java.math.BigDecimal;

/**
 * Проверка вхождения float в диапазон.
 * В бд value хранится в формате '(минимальное значение);(максимальное значение)', например '-99.99;99.99'
 */
public class FloatRangeAttributeValidationResolver implements AttributeValidationResolver<BigDecimal> {

    public static final String FLOAT_RANGE_EXCEPTION_CODE = "validation.range.err";

    private final Structure.Attribute attribute;
    private final BigDecimal min;
    private final BigDecimal max;

    public FloatRangeAttributeValidationResolver(Structure.Attribute attribute, BigDecimal min, BigDecimal max) {
        if (!FieldType.FLOAT.equals(attribute.getType()))
            throw new UserException("attribute.validation.type.invalid");
        this.attribute = attribute;
        this.min = min;
        this.max = max;
    }

    public FloatRangeAttributeValidationResolver(Structure.Attribute attribute, FloatRangeAttributeValidation validationValue) {
        this(attribute, validationValue.getMin(), validationValue.getMax());
    }

    @Override
    public Message resolve(BigDecimal value) {
        if (value == null) return null;
        boolean isLargerThanMin = min == null || value.compareTo(min) >= 0;
        boolean isLessThanMax = max == null || value.compareTo(max) <= 0;
        return isLargerThanMin && isLessThanMax
                ? null
                : new Message(FLOAT_RANGE_EXCEPTION_CODE, attribute.getName(), value);
    }
}
