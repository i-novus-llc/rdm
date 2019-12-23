package ru.inovus.ms.rdm.api.util;

import com.google.common.base.CaseFormat;

public class StringUtils {

    private static final String DOUBLE_QUOTE_CHAR = "\"";
    private static final String SINGLE_QUOTE_CHAR = "'";

    private StringUtils() {
    }

    public static String addDoubleQuotes(String value) {
        return DOUBLE_QUOTE_CHAR + value + DOUBLE_QUOTE_CHAR;
    }

    public static String addSingleQuotes(String value) {
        return SINGLE_QUOTE_CHAR + value + SINGLE_QUOTE_CHAR;
    }

    public static String camelCaseToSnakeCase(String s) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s);
    }

    public static String snakeCaseToCamelCase(String s) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, s);
    }

}
