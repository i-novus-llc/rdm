package ru.inovus.ms.rdm.n2o.util;

public class RdmUiUtil {

    /** Префикс для полей (колонок) справочника, чтобы отличать их от системных. */
    private static final String FIELD_PREFIX = "_refBook_";

    /** Разделитель названий частей в составном названии поля. */
    private static final char FIELD_PART_SEPARATOR = '_';

    /** Разделитель названия поля и названия свойства. */
    private static final char FIELD_PROPERTY_SEPARATOR = '.';

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
     * Добавление названия части к названию поля.
     *
     * @param fieldName название поля
     * @param partName  название части
     * @return Название поля с частью
     */
    public static String addFieldPart(String fieldName, String partName) {
        return fieldName + FIELD_PART_SEPARATOR + partName;
    }

    /**
     * Добавление названия свойства к названию поля.
     *
     * @param fieldName    название поля
     * @param propertyName название свойства
     * @return Название поля со свойством
     */
    public static String addFieldProperty(String fieldName, String propertyName) {
        return fieldName + FIELD_PROPERTY_SEPARATOR + propertyName;
    }
}
