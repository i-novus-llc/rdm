package ru.inovus.ms.rdm.model.validation;

import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class FloatRangeValidationValue extends AttributeValidationValue {

    private BigDecimal min;
    private BigDecimal max;

    public FloatRangeValidationValue() {
        super(AttributeValidationType.FLOAT_RANGE);
    }

    public FloatRangeValidationValue(BigDecimal min, BigDecimal max) {
        this();
        this.min = min;
        this.max = max;
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    @Override
    public String valuesToString() {
        return (min != null ? min.toPlainString() : "") + ";" +
                (max != null ? max.toPlainString() : "");
    }

    @Override
    public FloatRangeValidationValue valueFromString(String value) {
        if (value == null || !value.matches("^(-?\\d+\\.\\d+)*;(-?\\d+\\.\\d+)*$"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (StringUtils.isNotBlank(split[0]))
            min = new BigDecimal(split[0]);
        if (StringUtils.isNotBlank(split[1]))
            max = new BigDecimal(split[1]);
        return this;
    }
}
