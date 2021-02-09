package ru.i_novus.ms.rdm.api.model.diff;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.List;

public class RefBookDataDiff {

    private Page<DiffRowValue> rows;

    @JsonUnwrapped
    private RefBookAttributeDiff attributeDiff = new RefBookAttributeDiff();

    public RefBookDataDiff() {
        this(null, null);
    }

    public RefBookDataDiff(Page<DiffRowValue> rows,
                           List<String> oldAttributes, List<String> newAttributes, List<String> updatedAttributes) {

        this(rows, new RefBookAttributeDiff(oldAttributes, newAttributes, updatedAttributes));
    }

    public RefBookDataDiff(Page<DiffRowValue> rows, RefBookAttributeDiff attributeDiff) {

        this.rows = rows;
        this.attributeDiff = getOrCreateAttributeDiff(attributeDiff);
    }

    public Page<DiffRowValue> getRows() {
        return rows;
    }

    public void setRows(Page<DiffRowValue> rows) {
        this.rows = rows;
    }

    private static RefBookAttributeDiff getOrCreateAttributeDiff(RefBookAttributeDiff attributeDiff) {
        return attributeDiff == null ? new RefBookAttributeDiff() : attributeDiff;
    }

    public RefBookAttributeDiff getAttributeDiff() {
        return attributeDiff;
    }

    public void setAttributeDiff(RefBookAttributeDiff attributeDiff) {
        this.attributeDiff = getOrCreateAttributeDiff(attributeDiff);
    }
}