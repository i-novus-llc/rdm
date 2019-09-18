package ru.inovus.ms.rdm.n2o.model.compare;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

public class ComparableRow extends RdmComparable {

    private List<ComparableFieldValue> fieldValues;

    public ComparableRow() {
    }

    public ComparableRow(List<ComparableFieldValue> fieldValues, DiffStatusEnum status) {
        super(status);
        setFieldValues(fieldValues);
    }

    public List<ComparableFieldValue> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(List<ComparableFieldValue> fieldValues) {
        this.fieldValues = fieldValues;
        if (getStatus() == null
                &&
                fieldValues
                        .stream()
                        .anyMatch(fieldValue -> DiffStatusEnum.UPDATED.equals(fieldValue.getStatus())))
            setStatus(DiffStatusEnum.UPDATED);
    }

    public ComparableFieldValue getComparableFieldValue(String code) {
        return !isEmpty(fieldValues) ?
                fieldValues
                        .stream()
                        .filter(fieldValue -> code.equals(fieldValue.getComparableField().getCode()))
                        .findAny()
                        .orElse(null) :
                null;
    }

}
