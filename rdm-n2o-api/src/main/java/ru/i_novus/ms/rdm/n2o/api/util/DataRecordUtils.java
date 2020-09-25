package ru.i_novus.ms.rdm.n2o.api.util;

public class DataRecordUtils {

    /** Префикс для полей (колонок) справочника, чтобы отличать их от системных. */
    private static final String FIELD_PREFIX = "_refBook_";

    /** Разделитель наименований частей в составном наименовании поля. */
    private static final char FIELD_PART_SEPARATOR = '_';

    /** Разделитель наименования поля и наименования свойства. */
    private static final char FIELD_PROPERTY_SEPARATOR = '.';

    private DataRecordUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Добавление префикса к наименованию поля.
     *
     * @param fieldName наименование поля без префикса
     * @return Наименование поля с префиксом
     */
    public static String addPrefix(String fieldName) {
        return FIELD_PREFIX + fieldName;
    }

    /**
     * Удаление префикса из наименования поля.
     *
     * @param fieldName наименование поля с префиксом
     * @return Наименование поля без префикса
     */
    public static String deletePrefix(String fieldName) {

        if (hasPrefix(fieldName)) {
            fieldName = fieldName.replace(FIELD_PREFIX, "");
        }
        return fieldName;
    }

    /**
     * Проверка наличия префикса в наименовании поля.
     *
     * @param fieldName наименование поля
     * @return Результат проверки
     */
    public static boolean hasPrefix(String fieldName) {
        return fieldName != null && fieldName.startsWith(FIELD_PREFIX);
    }

    /**
     * Добавление наименования части к наименованию поля.
     *
     * @param fieldName наименование поля
     * @param partName  наименование части
     * @return Наименование поля с частью
     */
    public static String addFieldPart(String fieldName, String partName) {
        return fieldName + FIELD_PART_SEPARATOR + partName;
    }

    /**
     * Добавление наименования свойства к наименованию поля.
     *
     * @param fieldName    наименование поля
     * @param propertyName наименование свойства
     * @return Наименование поля со свойством
     */
    public static String addFieldProperty(String fieldName, String propertyName) {
        return fieldName + FIELD_PROPERTY_SEPARATOR + propertyName;
    }
}
