package ru.inovus.ms.rdm.n2o.model.compare;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.Objects;

public class ComparableFieldValue extends RdmComparable {

    private ComparableField comparableField;

    private Object oldValue;

    private Object newValue;

    public ComparableFieldValue() {
    }

    public ComparableFieldValue(ComparableField comparableField, Object oldValue, Object newValue, DiffStatusEnum status) {
        this.comparableField = comparableField;
        this.oldValue = oldValue;
        this.newValue = newValue;
        setStatus(status);
    }

    public ComparableFieldValue(ComparableField comparableField, DiffFieldValue diffFieldValue,
                                RowValue oldRowValue, RowValue newRowValue, DiffStatusEnum rowStatus) {
        String fieldCode = comparableField.getCode();

        this.comparableField = comparableField;
        if (comparableField.getStatus() == null) {
            if (diffFieldValue != null) {
                this.newValue = diffFieldValue.getNewValue();
                this.oldValue = diffFieldValue.getStatus() != null || oldRowValue == null
                        ? diffFieldValue.getOldValue()
                        : diffFieldValue.getNewValue();
            } else {
                this.oldValue = getValueFromRowValue(fieldCode, oldRowValue);
                this.newValue = getValueFromRowValue(fieldCode, newRowValue);
            }
        } else {
            switch (comparableField.getStatus()) {
                case DELETED:
                    this.oldValue = getValueFromRowValue(fieldCode, oldRowValue);
                    this.newValue = null;
                    break;
                case UPDATED:
                    this.oldValue = getValueFromRowValue(fieldCode, oldRowValue);
                    this.newValue = getValueFromRowValue(fieldCode, newRowValue);
                    break;
                case INSERTED:
                    this.oldValue = null;
                    this.newValue = getValueFromRowValue(fieldCode, newRowValue);
                    break;
                default:
                    break;
            }
        }
        setStatus(calculateFieldValueStatus(rowStatus, comparableField.getStatus(), diffFieldValue, oldValue, newValue));
    }

    private DiffStatusEnum calculateFieldValueStatus(DiffStatusEnum rowStatus, DiffStatusEnum fieldStatus,
                                                     DiffFieldValue diffFieldValue, Object oldValue, Object newValue) {
        if (DiffStatusEnum.DELETED.equals(rowStatus) || DiffStatusEnum.DELETED.equals(fieldStatus))
            return DiffStatusEnum.DELETED;
        else if (DiffStatusEnum.INSERTED.equals(rowStatus) || DiffStatusEnum.INSERTED.equals(fieldStatus))
            return DiffStatusEnum.INSERTED;
        else if ((diffFieldValue != null && DiffStatusEnum.UPDATED.equals(diffFieldValue.getStatus())) ||
                (DiffStatusEnum.UPDATED.equals(fieldStatus) &&
                        !Objects.equals(String.valueOf(oldValue), String.valueOf(newValue))))
            return DiffStatusEnum.UPDATED;
        return null;
    }

    private Object getValueFromRowValue(String fieldCode, RowValue rowValue) {
        return rowValue != null
                ? rowValue.getFieldValue(fieldCode).getValue()
                : null;
    }

    public ComparableField getComparableField() {
        return comparableField;
    }

    public void setComparableField(ComparableField comparableField) {
        this.comparableField = comparableField;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparableFieldValue that = (ComparableFieldValue) o;

        if (comparableField != null ? !comparableField.equals(that.comparableField) : that.comparableField != null)
            return false;
        if (oldValue != null ? that.oldValue == null || !oldValue.toString().equals(that.oldValue.toString()) : that.oldValue != null)
            return false;
        if (getStatus() != that.getStatus())
            return false;
        return newValue != null ? that.newValue != null && newValue.toString().equals(that.newValue.toString()) : that.newValue == null;
    }

    @Override
    public int hashCode() {

        return Objects.hash(comparableField, oldValue, newValue);
    }

}
