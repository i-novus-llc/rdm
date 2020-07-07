package ru.inovus.ms.rdm.api.util;

import com.google.common.base.CaseFormat;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.isEmpty;

public class StringUtils {

    private static final String DOUBLE_QUOTE_CHAR = "\"";
    private static final String SINGLE_QUOTE_CHAR = "'";

    private static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

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

    /** Преобразование строки в UUID. */
    public static UUID toUuid(String s) {

        if (!isEmpty(s))
            s = s.trim();

        if (isEmpty(s))
            return null;

        if (UUID_PATTERN.matcher(s).matches())
            return UUID.fromString(s);

        return NULL_UUID;
    }
}
