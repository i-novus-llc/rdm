package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.n2oapp.framework.api.metadata.meta.control.Control;

/**
 * Created by znurgaliev on 13.11.2018.
 */
public class DataColumn {
    @JsonProperty
    private String key;
    @JsonProperty
    private String name;
    @JsonProperty
    private boolean filterable;
    @JsonProperty
    private boolean resizable;
    @JsonProperty
    private boolean sortable;
    @JsonProperty
    private Control filterControl;

    public DataColumn() {
    }

    public DataColumn(String key, String name, boolean filterable, boolean resizable, boolean sortable, Control filterControl) {
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
