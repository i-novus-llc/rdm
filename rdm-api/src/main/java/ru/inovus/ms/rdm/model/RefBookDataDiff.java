package ru.inovus.ms.rdm.model;

import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.List;

public class RefBookDataDiff {

    private Page<DiffRowValue> rows;

    private List<String> newAttributes;
    private List<String> oldAttributes;
    private List<String> updatedAttributes;

    public RefBookDataDiff() {
    }

    public RefBookDataDiff(Page<DiffRowValue> rows, List<String> oldAttributes, List<String> newAttributes, List<String> updatedAttributes) {
        this.rows = rows;
        this.oldAttributes = oldAttributes;
        this.newAttributes = newAttributes;
        this.updatedAttributes = updatedAttributes;
    }

    public Page<DiffRowValue> getRows() {
        return rows;
    }

    public void setRows(Page<DiffRowValue> rows) {
        this.rows = rows;
    }

    public List<String> getNewAttributes() {
        return newAttributes;
    }

    public void setNewAttributes(List<String> newAttributes) {
        this.newAttributes = newAttributes;
    }

    public List<String> getOldAttributes() {
        return oldAttributes;
    }

    public void setOldAttributes(List<String> oldAttributes) {
        this.oldAttributes = oldAttributes;
    }

    public List<String> getUpdatedAttributes() {
        return updatedAttributes;
    }

    public void setUpdatedAttributes(List<String> updatedAttributes) {
        this.updatedAttributes = updatedAttributes;
    }

}