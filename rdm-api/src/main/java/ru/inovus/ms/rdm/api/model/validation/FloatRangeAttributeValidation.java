package ru.inovus.ms.rdm.api.model.validation;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class FloatRangeAttributeValidation extends AttributeValidation {

    private BigDecimal min;
    private BigDecimal max;

    public FloatRangeAttributeValidation() {
        super(AttributeValidationType.FLOAT_RANGE);
    }

    public FloatRangeAttributeValidation(BigDecimal min, BigDecimal max) {
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
    public FloatRangeAttributeValidation valueFromString(String value) {
        if (value == null || !value.matches("^(-?\\d+\\.\\d+)*;(-?\\d+\\.\\d+)*$"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (!StringUtils.isEmpty(split[0]))
            min = new BigDecimal(split[0]);
        if (!StringUtils.isEmpty(split[1]))
            max = new BigDecimal(split[1]);
        return this;
    }
}
