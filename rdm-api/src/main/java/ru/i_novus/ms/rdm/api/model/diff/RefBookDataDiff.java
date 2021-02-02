package ru.i_novus.ms.rdm.api.model.diff;

import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.List;

public class RefBookDataDiff extends RefBookAttributeDiff {

    private Page<DiffRowValue> rows;

    public RefBookDataDiff() {
    }

    public RefBookDataDiff(Page<DiffRowValue> rows, List<String> oldAttributes, List<String> newAttributes, List<String> updatedAttributes) {

        super(oldAttributes, newAttributes, updatedAttributes);

        this.rows = rows;
    }

    public RefBookDataDiff(Page<DiffRowValue> rows, RefBookAttributeDiff attributeDiff) {

        this(rows, attributeDiff.getOldAttributes(), attributeDiff.getNewAttributes(), attributeDiff.getUpdatedAttributes());
    }

    public Page<DiffRowValue> getRows() {
        return rows;
    }

    public void setRows(Page<DiffRowValue> rows) {
        this.rows = rows;
    }
}