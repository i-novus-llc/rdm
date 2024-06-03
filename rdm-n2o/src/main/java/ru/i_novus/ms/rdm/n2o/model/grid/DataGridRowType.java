package ru.i_novus.ms.rdm.n2o.model.grid;

/**
 * Тип записи справочника.
 * <p>
 * Используется для выделения отдельных записей в DataGrid.
 */
public enum DataGridRowType {

    DEFAULT,    // Обычная запись
    CONFLICTED  // Конфликтная запись
    ;
}