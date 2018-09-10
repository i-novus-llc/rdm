package ru.inovus.ms.rdm.model;

import java.util.Objects;

public class PassportAttributeDiff {

    private PassportAttribute passportAttribute;
    private String leftValue;
    private String rightValue;

    public PassportAttributeDiff() {
    }

    public PassportAttributeDiff(PassportAttribute passportAttribute, String sourceValue, String rightValue) {
        this.passportAttribute = passportAttribute;
        this.leftValue = sourceValue;
        this.rightValue = rightValue;
    }

    public PassportAttribute getPassportAttribute() {
        return passportAttribute;
    }

    public void setPassportAttribute(PassportAttribute passportAttribute) {
        this.passportAttribute = passportAttribute;
    }

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassportAttributeDiff passportAttributeDiff = (PassportAttributeDiff) o;
        if (!Objects.equals(passportAttribute, passportAttributeDiff.passportAttribute)) return false;
        if (!(leftValue == null && passportAttributeDiff.leftValue == null || Objects.equals(leftValue, passportAttributeDiff.leftValue)))
            return false;
        return (rightValue == null && passportAttributeDiff.rightValue == null || Objects.equals(rightValue, passportAttributeDiff.rightValue));
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (passportAttribute != null ? passportAttribute.hashCode() : 0);
        result = 31 * result + (leftValue != null ? leftValue.hashCode() : 0);
        result = 31 * result + (rightValue != null ? rightValue.hashCode() : 0);
        return result;
    }

}