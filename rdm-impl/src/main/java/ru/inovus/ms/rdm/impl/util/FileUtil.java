package ru.inovus.ms.rdm.impl.util;

import org.apache.commons.io.FilenameUtils;

import static org.springframework.util.StringUtils.isEmpty;

public class FileUtil {

    public static final String FILE_EXTENSION_INVALID_EXCEPTION_CODE = "file.extension.invalid";

    private FileUtil() { throw new UnsupportedOperationException(); }

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
