package ru.i_novus.ms.rdm.impl.util;

import org.apache.commons.io.FilenameUtils;

import static org.springframework.util.StringUtils.isEmpty;

public final class FileUtil {

    private FileUtil() {
        // Nothing to do.
    }

    /**
     * Получение расширения файла для выбора действий с этим файлом.
     *
     * @param filename полное название файла
     * @return Расширение файла в верхнем регистре, если есть, в противном случае - пустая строка
     */
    public static String getExtension(String filename) {

        String result = FilenameUtils.getExtension(filename);
        if (!isEmpty(result))
            result = result.trim();

        return !isEmpty(result) ? result.toUpperCase() : "";
    }
}
