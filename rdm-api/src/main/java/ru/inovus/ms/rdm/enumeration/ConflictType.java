package ru.inovus.ms.rdm.enumeration;

/**
 * Тип конфликта.
 */
public enum ConflictType {

    // Изменения в данных:
    UPDATED,            // Запись обновлена (ссылка ведёт на обновлённую запись)
    DELETED,            // Запись удалена (ссылка ведёт на удалённую запись)

    // Изменения в структуре:
    ALTERED,            // Атрибут изменён, добавлен, удалён (ссылка ведёт на запись с изменённым hash)
    DISPLAY_DAMAGED     // Код атрибута отсутствует (ссылка содержит отсутствующий код в displayExpression)

}