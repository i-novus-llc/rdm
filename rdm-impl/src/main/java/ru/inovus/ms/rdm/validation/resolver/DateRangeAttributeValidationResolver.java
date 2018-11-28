package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.DateRangeValidationValue;

import java.time.LocalDate;

/**
 * Проверка вхождения даты в диапазон.
 * В бд value хранится в формате '(минимальное значение);(максимальное значение)', например '01.01.2018;31.12.2018'
 */
public class DateRangeAttributeValidationResolver implements AttributeValidationResolver<LocalDate> {

    public static final String DATE_RANGE_EXCEPTION_CODE = "validation.range.err";

    private final Structure.Attribute attribute;
    private final LocalDate min;
    private final LocalDate max;

    public DateRangeAttributeValidationResolver(Structure.Attribute attribute, LocalDate min, LocalDate max) {
        if (!FieldType.DATE.equals(attribute.getType()))
            throw new UserException("attribute.validation.type.invalid");
        this.attribute = attribute;
        this.min = min;
        this.max = max;
    }

    public DateRangeAttributeValidationResolver(Structure.Attribute attribute, DateRangeValidationValue validationValue) {
        this(attribute, validationValue.getMin(), validationValue.getMax());
    }

    @Override
    public Message resolve(LocalDate value) {
        if (value == null) return null;
        boolean isLargerThanMin = min == null || value.compareTo(min) >= 0;
        boolean isLessThanMax = max == null || value.compareTo(max) <= 0;
        return isLargerThanMin && isLessThanMax ? null : new Message(DATE_RANGE_EXCEPTION_CODE, attribute, value);
    }
}
