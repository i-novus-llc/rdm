package ru.inovus.ms.rdm.n2o.util;

public class StringUtils {

    private static final String DOUBLE_QUOTE_CHAR = "\"";
    private static final String SINGLE_QUOTE_CHAR = "\"";

    private StringUtils() {
    }

    public static String addDoubleQuotes(String value) {
        return DOUBLE_QUOTE_CHAR + value + DOUBLE_QUOTE_CHAR;
    }

    public static String addSingleQuotes(String value) {
        return SINGLE_QUOTE_CHAR + value + SINGLE_QUOTE_CHAR;
    }
}
