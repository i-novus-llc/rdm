package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

import java.util.Objects;

public class ComparableFieldValue {

    private ComparableField comparableField;

    private Object oldValue;

    private Object newValue;

    public ComparableFieldValue() {
    }

    public ComparableFieldValue(ComparableField comparableField, Object oldValue, Object newValue) {
        this.comparableField = comparableField;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public DiffStatusEnum getFieldValueStatus() {
        if (oldValue == null)
            return newValue == null ? null : DiffStatusEnum.INSERTED;
        if (newValue == null)
            return DiffStatusEnum.DELETED;
        return oldValue.toString().equals(newValue.toString()) ? null : DiffStatusEnum.UPDATED;
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
        return newValue != null ? that.newValue != null && newValue.toString().equals(that.newValue.toString()) : that.newValue == null;
    }

    @Override
    public int hashCode() {

        return Objects.hash(comparableField, oldValue, newValue);
    }

}
