package ru.inovus.ms.rdm.enumeration;

/**
 * Тип конфликта.
 */
public enum ConflictType {

    // Изменения в данных:
    UPDATED,            // Запись обновлена
    DELETED,            // Запись удалена

    // Изменения в структуре:
    ALTERED             // Атрибут изменён, добавлен, удалён

}