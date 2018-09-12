package ru.inovus.ms.rdm.model;

import java.util.Objects;

public class PassportAttributeDiff {

    private PassportAttribute passportAttribute;
    private String firstValue;
    private String secondValue;

    public PassportAttributeDiff() {
    }

    public PassportAttributeDiff(PassportAttribute passportAttribute, String firstValue, String secondValue) {
        this.passportAttribute = passportAttribute;
        this.firstValue = firstValue;
        this.secondValue = secondValue;
    }

    public PassportAttribute getPassportAttribute() {
        return passportAttribute;
    }

    public void setPassportAttribute(PassportAttribute passportAttribute) {
        this.passportAttribute = passportAttribute;
    }

    public String getFirstValue() {
        return firstValue;
    }

    public void setFirstValue(String firstValue) {
        this.firstValue = firstValue;
    }

    public String getSecondValue() {
        return secondValue;
    }

    public void setSecondValue(String secondValue) {
        this.secondValue = secondValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassportAttributeDiff passportAttributeDiff = (PassportAttributeDiff) o;
        if (!Objects.equals(passportAttribute, passportAttributeDiff.passportAttribute)) return false;
        if (!(firstValue == null && passportAttributeDiff.firstValue == null || Objects.equals(firstValue, passportAttributeDiff.firstValue)))
            return false;
        return (secondValue == null && passportAttributeDiff.secondValue == null || Objects.equals(secondValue, passportAttributeDiff.secondValue));
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (passportAttribute != null ? passportAttribute.hashCode() : 0);
        result = 31 * result + (firstValue != null ? firstValue.hashCode() : 0);
        result = 31 * result + (secondValue != null ? secondValue.hashCode() : 0);
        return result;
    }

}