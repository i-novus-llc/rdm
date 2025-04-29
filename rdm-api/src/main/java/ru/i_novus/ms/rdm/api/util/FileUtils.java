package ru.i_novus.ms.rdm.api.util;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static ru.i_novus.ms.rdm.api.util.StringUtils.isEmpty;

public final class FileUtils {

    private static final String EMPTY_EXTENSION = "";

    private FileUtils() {
        // Nothing to do.
    }

    /**
     * Получение последнего расширения файла из наименования файла.
     *
     * @param filename наименование файла
     * @return Последнее расширение файла, пустая строка при отсутствии наименования или расширения
     */
    public static String getLastExtension(String filename) {

        final String extension = FilenameUtils.getExtension(filename);
        return extension != null ? extension : EMPTY_EXTENSION;
    }

    /**
     * Получение расширения файла со справочником из наименования файла.
     * <p>
     * Для корректной обработки файла со справочником используется
     * последнее расширение в верхнем регистре без крайних пробелов.
     *
     * @param filename наименование файла
     * @return Последнее расширение файла в верхнем регистре без крайних пробелов или пустая строка
     */
    public static String getRefBookFileExtension(String filename) {

        return getLastExtension(filename).trim().toUpperCase();
    }

    /**
     * Получение списка расширений файла из наименования файла.
     *
     * @param filename наименование файла
     * @return Расширение файла, пустая строка при отсутствии расширения, null при отсутствии наименования файла
     */
    public static List<String> getExtensions(String filename) {

        if (isEmpty(filename))
            return emptyList();

        final List<String> list = new ArrayList<>();
        String extension = getLastExtension(filename);
        while (!isEmpty(extension)) {
            list.add(extension);

            filename = filename.substring(0, filename.length() - extension.length() - 1);
            extension = getLastExtension(filename);
        }

        return list;
    }
}
