package ru.inovus.ms.rdm.model.validation;

import net.n2oapp.platform.i18n.UserException;
import org.apache.commons.lang3.StringUtils;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.time.LocalDate;

import static ru.inovus.ms.rdm.util.TimeUtils.format;

public class DateRangeValidationValue extends AttributeValidationValue {

    private LocalDate min;
    private LocalDate max;

    public DateRangeValidationValue() {
        super(AttributeValidationType.DATE_RANGE);
    }

    public DateRangeValidationValue(LocalDate min, LocalDate max) {
        this();
        this.min = min;
        this.max = max;
    }

    public LocalDate getMin() {
        return min;
    }

    public void setMin(LocalDate min) {
        this.min = min;
    }

    public LocalDate getMax() {
        return max;
    }

    public void setMax(LocalDate max) {
        this.max = max;
    }

    @Override
    public String valuesToString() {
        return (min != null ? format(min) : "") + ";" + (max != null ? format(max) : "");
    }

    @Override
    public DateRangeValidationValue valueFromString(String value) {
        if (value == null || !value.matches("^(\\d{2}\\.\\d{2}\\.\\d{4})*;(\\d{2}\\.\\d{2}\\.\\d{4})*$"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (StringUtils.isNotBlank(split[0]))
            min = TimeUtils.parseLocalDate(split[0]);
        if (StringUtils.isNotBlank(split[1]))
            max = TimeUtils.parseLocalDate(split[1]);
        return this;
    }
}
