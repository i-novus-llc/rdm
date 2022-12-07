package ru.i_novus.ms.rdm.n2o.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.n2oapp.framework.api.metadata.meta.control.Control;

/**
 * Колонка для DataGrid.
 *
 * Created by znurgaliev on 13.11.2018.
 */
public class DataGridColumn {

    /** Обозначение. */
    @JsonProperty
    private String key;
    /** Наименование. */
    @JsonProperty
    private String name;

    /** Фильтруемость. */
    @JsonProperty
    private boolean filterable;
    /** Изменяемость по ширине. */
    @JsonProperty
    private boolean resizable;
    /** Сортируемость. */
    @JsonProperty
    private boolean sortable;

    /** Элемент управления для фильтрации. */
    @JsonProperty
    private Control filterControl;

    public DataGridColumn() {
    }

    public DataGridColumn(String key, String name,
                          boolean filterable, boolean resizable, boolean sortable,
                          Control filterControl) {
        this.key = key;
        this.name = name;

        this.filterable = filterable;
        this.resizable = resizable;
        this.sortable = sortable;

        this.filterControl = filterControl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public Control getFilterControl() {
        return filterControl;
    }

    public void setFilterControl(Control filterControl) {
        this.filterControl = filterControl;
    }
}
