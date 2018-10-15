package ru.inovus.ms.rdm.model.compare;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.Objects;

public class ComparableFieldValue extends RdmComparable {

    private ComparableField comparableField;

    private Object oldValue;

    private Object newValue;

    private DiffStatusEnum diffStatus;

    public ComparableFieldValue() {
    }

    public ComparableFieldValue(ComparableField comparableField, Object oldValue, Object newValue) {
        this.comparableField = comparableField;
        this.oldValue = oldValue;
        this.newValue = newValue;
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
        if (DiffStatusEnum.DELETED.equals(rowStatus) || DiffStatusEnum.DELETED.equals(comparableField.getStatus()))
            diffStatus = DiffStatusEnum.DELETED;
        else if (DiffStatusEnum.INSERTED.equals(rowStatus) || DiffStatusEnum.INSERTED.equals(comparableField.getStatus()))
            diffStatus = DiffStatusEnum.INSERTED;
        else if ((diffFieldValue != null && DiffStatusEnum.UPDATED.equals(diffFieldValue.getStatus())) ||
                (DiffStatusEnum.UPDATED.equals(comparableField.getStatus()) && !Objects.equals(String.valueOf(oldValue), String.valueOf(newValue))))
            diffStatus = DiffStatusEnum.UPDATED;

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

    public DiffStatusEnum getDiffStatus() {
        return diffStatus;
    }

    public void setDiffStatus(DiffStatusEnum diffStatus) {
        this.diffStatus = diffStatus;
    }

    @Override
    public int hashCode() {

        return Objects.hash(comparableField, oldValue, newValue);
    }

}
