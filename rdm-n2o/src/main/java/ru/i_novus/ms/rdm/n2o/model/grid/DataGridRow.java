package ru.i_novus.ms.rdm.n2o.model.grid;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Запись DataGrid.
 * <p>
 * Набор значений с конфигурацией колонок.
 */
@Getter
@Setter
@SuppressWarnings("WeakerAccess")
public class DataGridRow {

    /**
     * Идентификатор записи.
     */
    @JsonProperty
    private Long id;

    /**
     * Колонки.
     */
    @JsonProperty
    private DataGridColumnsConfig columnsConfig;

    /**
     * Содержимое (строка).
     */
    @JsonProperty
    private Map<String, Object> row;

    public DataGridRow(Long id, Map<String, Object> row) {
        this.id = id;
        this.row = row;
    }
}
