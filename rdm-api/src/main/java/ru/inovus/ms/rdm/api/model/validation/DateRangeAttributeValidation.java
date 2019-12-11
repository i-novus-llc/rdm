package ru.inovus.ms.rdm.api.model.validation;

import net.n2oapp.platform.i18n.UserException;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import java.time.LocalDate;

import static ru.inovus.ms.rdm.api.util.TimeUtils.format;

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
        if (value == null || !value.matches("(\\d{2}\\.\\d{2}\\.\\d{4})?;(\\d{2}\\.\\d{2}\\.\\d{4})?"))
            throw new UserException("attribute.validation.value.invalid");
        String[] split = value.split(";");
        if (!split[0].isEmpty())
            min = LocalDate.parse(split[0], TimeUtils.STRICT_EUROPEAN_FORMATTER);
        if (!split[1].isEmpty())
            max = LocalDate.parse(split[1], TimeUtils.STRICT_EUROPEAN_FORMATTER);
        return this;
    }
}
