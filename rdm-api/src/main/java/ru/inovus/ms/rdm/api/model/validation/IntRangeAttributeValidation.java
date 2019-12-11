package ru.inovus.ms.rdm.api.model.validation;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;

import java.math.BigInteger;

public class IntRangeAttributeValidation extends AttributeValidation {

    private BigInteger min;
    private BigInteger max;

    public IntRangeAttributeValidation() {
        super(AttributeValidationType.INT_RANGE);
    }

    public IntRangeAttributeValidation(BigInteger min, BigInteger max) {
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
    public IntRangeAttributeValidation valueFromString(String value) {
        if (value == null || !value.matches("(-?\\d+)?;(-?\\d+)?"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (!StringUtils.isEmpty(split[0]))
            min = new BigInteger(split[0]);
        if (!StringUtils.isEmpty(split[1]))
            max = new BigInteger(split[1]);
        return this;
    }
}
