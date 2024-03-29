package ru.i_novus.ms.rdm.api.util;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public final class StringUtils {

    public static final String ESCAPE_CHAR = "\\";
    public static final String DOUBLE_QUOTE_CHAR = "\"";
    public static final String SINGLE_QUOTE_CHAR = "'";

    static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    private static final String JOIN_NUMERATED_DEFAULT_FORMAT = "%1$d) %2$s";
    private static final String JOIN_NUMERATED_DEFAULT_DELIMITER = "\n";

    private StringUtils() {
        // Nothing to do.
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String addDoubleQuotes(String value) {
        return DOUBLE_QUOTE_CHAR + value + DOUBLE_QUOTE_CHAR;
    }

    public static String addSingleQuotes(String value) {
        return SINGLE_QUOTE_CHAR + value + SINGLE_QUOTE_CHAR;
    }

    public static String toDoubleQuotes(String value) {
        return DOUBLE_QUOTE_CHAR + value.replace(DOUBLE_QUOTE_CHAR, ESCAPE_CHAR + DOUBLE_QUOTE_CHAR) + DOUBLE_QUOTE_CHAR;
    }

    /** Преобразование строки в UUID. */
    // used in: asyncLog.query.xml
    public static UUID toUuid(String s) {

        if (!isEmpty(s))
            s = s.trim();

        if (isEmpty(s))
            return null;

        if (UUID_PATTERN.matcher(s).matches())
            return UUID.fromString(s);

        return NULL_UUID;
    }

    /**
     * Соединение строк в одну с автонумерацией в формате по умолчанию.
     *
     * @param values список соединяемых строк
     * @return Соединённая строка
     */
    public static String joinNumerated(List<String> values) {

        return joinNumerated(values, JOIN_NUMERATED_DEFAULT_FORMAT, JOIN_NUMERATED_DEFAULT_DELIMITER);
    }

    /**
     * Соединение строк в одну с автонумерацией.
     *
     * @param values    список соединяемых строк
     * @param format    формат автонумерации строки
     * @param delimiter разделитель строк при соединении
     * @return Соединённая строка
     */
    public static String joinNumerated(List<String> values, String format, String delimiter) {

        return IntStream.rangeClosed(1, values.size())
                .mapToObj(index -> String.format(format, index, values.get(index - 1)))
                .collect(joining(delimiter));
    }
}
