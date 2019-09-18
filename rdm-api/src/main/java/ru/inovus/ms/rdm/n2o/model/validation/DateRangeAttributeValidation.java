package ru.inovus.ms.rdm.n2o.model.validation;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.util.StringUtils;
import ru.inovus.ms.rdm.n2o.util.TimeUtils;

import java.time.LocalDate;

import static ru.inovus.ms.rdm.n2o.util.TimeUtils.format;

public class DateRangeAttributeValidation extends AttributeValidation {

    private LocalDate min;
    private LocalDate max;

    public DateRangeAttributeValidation() {
        super(AttributeValidationType.DATE_RANGE);
    }

    public DateRangeAttributeValidation(LocalDate min, LocalDate max) {
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
    public DateRangeAttributeValidation valueFromString(String value) {
        if (value == null || !value.matches("^(\\d{2}\\.\\d{2}\\.\\d{4})*;(\\d{2}\\.\\d{2}\\.\\d{4})*$"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (!StringUtils.isEmpty(split[0]))
            min = TimeUtils.parseLocalDate(split[0]);
        if (!StringUtils.isEmpty(split[1]))
            max = TimeUtils.parseLocalDate(split[1]);
        return this;
    }
}
