package ru.i_novus.ms.rdm.api.enumeration;

/**
 * Тип версии для поиска источника данных справочника.
 */
public enum RefBookSourceType {
    ALL,                // все версии
    ACTUAL,             // актуальная (на текущую дату) версия
    DRAFT,              // версия-черновик
    LAST_PUBLISHED,     // последняя опубликованная версия
    LAST_VERSION        // последняя версия (опубликованная или черновик)
}
