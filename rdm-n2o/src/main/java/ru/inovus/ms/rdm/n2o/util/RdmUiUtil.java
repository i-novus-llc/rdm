package ru.inovus.ms.rdm.n2o.util;

public class RdmUiUtil {

    /** Префикс для полей (колонок) справочника, чтобы отличать их от системных. */
    private static final String FIELD_PREFIX = "_refBook_";

    /** Разделитель названия и суффикса. */
    private static final char SUFFIX_SEPARATOR = '.';

    private RdmUiUtil() {
    }

    /**
     * Добавление префикса к названию поля.
     *
     * @param string название поля без префикса
     * @return Название поля с префиксом
     */
    public static String addPrefix(String string) {
        return FIELD_PREFIX + string;
    }

    /**
     * Удаление префикса из названия поля.
     *
     * @param string название поля с префиксом
     * @return Название поля без префикса
     */
    public static String deletePrefix(String string) {
        if (string != null
                && string.startsWith(FIELD_PREFIX)) {
            string = string.replace(FIELD_PREFIX, "");
        }
        return string;
    }

    /**
     * Добавление суффикса к названию.
     *
     * @param name   название
     * @param suffix суффикс
     * @return Идентификатор
     */
    public static String addSuffix(String name, String suffix) {
        return name + SUFFIX_SEPARATOR + suffix;
    }
}
