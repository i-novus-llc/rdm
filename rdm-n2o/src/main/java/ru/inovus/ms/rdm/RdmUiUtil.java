package ru.inovus.ms.rdm;

public class RdmUiUtil {

    public static final String FIELD_PREFIX = "_refBook_";

    private RdmUiUtil() {
    }

    /**
     * Префикс для пользовательских полей, для отличия их от системных
     *
     * @param string название поля
     * @return поле с префиксом
     */
    public static String addPrefix(String string) {
        return FIELD_PREFIX + string;
    }

    public static String deletePrefix(String string) {
        if (string != null && string.startsWith(FIELD_PREFIX))
            string = string.replace(FIELD_PREFIX, "");
        return string;
    }
}
