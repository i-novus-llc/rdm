package ru.inovus.ms.rdm.enumeration;

/**
 * Тип версии для поиска источника данных справочника.
 */
public enum RefBookSourceType {
    ACTUAL,             // актуальная версия
    DRAFT,              // черновик
    LAST_PUBLISHED,     // последняя опубликованная версия
    LAST_VERSION        // последняя созданная версия или черновик
}
