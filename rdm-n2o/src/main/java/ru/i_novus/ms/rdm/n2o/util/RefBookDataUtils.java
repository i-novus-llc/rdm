package ru.i_novus.ms.rdm.n2o.util;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public final class RefBookDataUtils {

    private static final String DATA_FILTER_BOOL_IS_INVALID_EXCEPTION_CODE = "data.filter.bool.is.invalid";

    // to-do: Вынести список значений в rdmui.properties, а здесь преобразовать в regex (с фильтрацией корректных символов!)
    private static final String BOOL_TRUE_REGEX = "true|t|y|yes|yeah|д|да|истина|правда";
    private static final Pattern BOOL_TRUE_PATTERN = Pattern.compile(BOOL_TRUE_REGEX);
    private static final String BOOL_FALSE_REGEX = "false|f|n|no|nah|н|нет|ложь|неправда";
    private static final Pattern BOOL_FALSE_PATTERN = Pattern.compile(BOOL_FALSE_REGEX);

    private RefBookDataUtils() {
        // Nothing to do.
    }

    public static Serializable castFilterValue(Structure.Attribute attribute, Serializable value) {

        if (value == null)
            return null;

        switch (attribute.getType()) {
            case INTEGER: return castInteger(value);
            case FLOAT: return castFloat(value);
            case DATE: return castDate(value);
            case BOOLEAN: return castBoolean(value);
            default: return value;
        }
    }

    public static BigInteger castInteger(Serializable value) {

        if (value instanceof BigInteger)
            return (BigInteger) value;

        if (value instanceof Integer)
            return BigInteger.valueOf((Integer) value);

        return parseInteger((String) value);
    }

    public static BigInteger parseInteger(String value) {
        return new BigInteger(value);
    }

    public static BigDecimal castFloat(Serializable value) {

        if (value instanceof BigDecimal)
            return (BigDecimal) value;

        if (value instanceof Double)
            return BigDecimal.valueOf((Double) value);

        if (value instanceof BigInteger)
            return new BigDecimal((BigInteger) value);

        if (value instanceof Integer)
            return BigDecimal.valueOf((Integer) value);

        return parseFloat((String) value);
    }

    public static BigDecimal parseFloat(String value) {
        return new BigDecimal(value.replace(",", ".").trim());
    }

    public static LocalDate castDate(Serializable value) {

        if (value instanceof LocalDate)
            return (LocalDate) value;

        if (value instanceof LocalDateTime)
            return ((LocalDateTime) value).toLocalDate();

        return parseDate((String) value);
    }

    public static LocalDate parseDate(String value) {
        return LocalDate.parse(value, TimeUtils.DATE_TIME_PATTERN_EUROPEAN_FORMATTER);
    }

    public static Boolean castBoolean(Serializable value) {

        if (value instanceof Boolean)
            return (Boolean) value;

        return parseBoolean((String) value);
    }

    public static Boolean parseBoolean(String value) {

        String stringValue = value.toLowerCase();
        if (StringUtils.isEmpty(stringValue))
            return null;

        if (BOOL_TRUE_PATTERN.matcher(stringValue).matches())
            return Boolean.TRUE;

        if (BOOL_FALSE_PATTERN.matcher(stringValue).matches())
            return Boolean.FALSE;

        throw new IllegalArgumentException(DATA_FILTER_BOOL_IS_INVALID_EXCEPTION_CODE);
    }
}
