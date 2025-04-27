package ru.i_novus.ms.rdm.api.util;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static ru.i_novus.ms.rdm.api.util.StringUtils.isEmpty;

public final class FileUtils {

    private FileUtils() {
        // Nothing to do.
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
        String extension = FilenameUtils.getExtension(filename);
        while (!isEmpty(extension)) {
            list.add(extension);

            filename = filename.substring(0, filename.length() - extension.length() - 1);
            extension = FilenameUtils.getExtension(filename);
        }

        return list;
    }
}
