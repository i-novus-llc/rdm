package ru.inovus.ms.rdm.n2o.model.diff;

import ru.inovus.ms.rdm.n2o.model.version.PassportAttribute;

import java.util.Objects;

public class PassportAttributeDiff {

    private PassportAttribute passportAttribute;
    private String oldValue;
    private String newValue;

    public PassportAttributeDiff() {
    }

    public PassportAttributeDiff(PassportAttribute passportAttribute, String oldValue, String newValue) {
        this.passportAttribute = passportAttribute;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public PassportAttribute getPassportAttribute() {
        return passportAttribute;
    }

    public void setPassportAttribute(PassportAttribute passportAttribute) {
        this.passportAttribute = passportAttribute;
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
        if (!Objects.equals(passportAttribute, passportAttributeDiff.passportAttribute)) return false;
        if (!(oldValue == null && passportAttributeDiff.oldValue == null || Objects.equals(oldValue, passportAttributeDiff.oldValue)))
            return false;
        return (newValue == null && passportAttributeDiff.newValue == null || Objects.equals(newValue, passportAttributeDiff.newValue));
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (passportAttribute != null ? passportAttribute.hashCode() : 0);
        result = 31 * result + (oldValue != null ? oldValue.hashCode() : 0);
        result = 31 * result + (newValue != null ? newValue.hashCode() : 0);
        return result;
    }

}