package ru.inovus.ms.rdm.model.validation;

import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;

public class IntRangeValidationValue extends AttributeValidationValue {

    private BigInteger min;
    private BigInteger max;

    public IntRangeValidationValue() {
        super(AttributeValidationType.INT_RANGE);
    }

    public IntRangeValidationValue(BigInteger min, BigInteger max) {
        this();
        this.min = min;
        this.max = max;
    }

    public BigInteger getMin() {
        return min;
    }

    public void setMin(BigInteger min) {
        this.min = min;
    }

    public BigInteger getMax() {
        return max;
    }

    public void setMax(BigInteger max) {
        this.max = max;
    }

    @Override
    public String valuesToString() {
        return (min != null ? min : "") + ";" +
                (max != null ? max : "");
    }

    @Override
    public IntRangeValidationValue valueFromString(String value) {
        if (value == null || !value.matches("^(-?\\d+)*;(-?\\d+)*$"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (StringUtils.isNotBlank(split[0]))
            min = new BigInteger(split[0]);
        if (StringUtils.isNotBlank(split[1]))
            max = new BigInteger(split[1]);
        return this;
    }
}
