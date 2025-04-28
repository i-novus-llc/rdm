package ru.i_novus.ms.rdm.n2o.validation;

import ru.i_novus.ms.rdm.api.enumeration.FileUsageTypeEnum;

public interface FileValidation {

    /**
     * Валидация наименования файла.
     *
     * @param filename наименование файла
     */
    void validateName(String filename);

    /**
     * Валидация расширений файла.
     *
     * @param filename наименование файла
     */
    void validateExtensions(String filename);

    /**
     * Валидация расширения по списку.
     *
     * @param extension     расширение
     * @param fileUsageType тип использования файла
     */
    void validateExtensionByUsage(String extension, FileUsageTypeEnum fileUsageType);

    /**
     * Валидация размера файла.
     *
     * @param fileSize размер файла
     */
    void validateSize(long fileSize);
}
