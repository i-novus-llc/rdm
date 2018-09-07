package ru.inovus.ms.rdm.model;

import java.util.Objects;

public class PassportAttributeDiff {

    private String attributeName;
    private String oldValue;
    private String newValue;

    public PassportAttributeDiff() {
    }

    public PassportAttributeDiff(String attributeName, String oldValue, String newValue) {
        this.attributeName = attributeName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassportAttributeDiff passportAttributeDiff = (PassportAttributeDiff) o;
        if (!Objects.equals(attributeName, passportAttributeDiff.attributeName)) return false;
        if (!(oldValue == null && passportAttributeDiff.oldValue == null || Objects.equals(oldValue, passportAttributeDiff.oldValue)))
            return false;
        return (newValue == null && passportAttributeDiff.newValue == null || Objects.equals(newValue, passportAttributeDiff.newValue));
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (attributeName != null ? attributeName.hashCode() : 0);
        result = 31 * result + (oldValue != null ? oldValue.hashCode() : 0);
        result = 31 * result + (newValue != null ? newValue.hashCode() : 0);
        return result;
    }

}