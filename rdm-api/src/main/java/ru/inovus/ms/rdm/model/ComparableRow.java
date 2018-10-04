package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

import java.util.List;

public class ComparableRow extends RdmComparable {

    private List<ComparableFieldValue> fieldValues;

    public ComparableRow() {
    }

    public ComparableRow(List<ComparableFieldValue> fieldValues, DiffStatusEnum status) {
        super(status);
        this.fieldValues = fieldValues;
    }

    public List<ComparableFieldValue> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(List<ComparableFieldValue> fieldValues) {
        this.fieldValues = fieldValues;
    }

}
