package ru.inovus.ms.rdm.model;

import java.util.Objects;

public class PassportAttributeDiff {

    private PassportAttribute passportAttribute;
    private String sourceValue;
    private String targetValue;

    public PassportAttributeDiff() {
    }

    public PassportAttributeDiff(PassportAttribute passportAttribute, String sourceValue, String targetValue) {
        this.passportAttribute = passportAttribute;
        this.sourceValue = sourceValue;
        this.targetValue = targetValue;
    }

    public PassportAttribute getPassportAttribute() {
        return passportAttribute;
    }

    public void setPassportAttribute(PassportAttribute passportAttribute) {
        this.passportAttribute = passportAttribute;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassportAttributeDiff passportAttributeDiff = (PassportAttributeDiff) o;
        if (!Objects.equals(passportAttribute, passportAttributeDiff.passportAttribute)) return false;
        if (!(sourceValue == null && passportAttributeDiff.sourceValue == null || Objects.equals(sourceValue, passportAttributeDiff.sourceValue)))
            return false;
        return (targetValue == null && passportAttributeDiff.targetValue == null || Objects.equals(targetValue, passportAttributeDiff.targetValue));
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (passportAttribute != null ? passportAttribute.hashCode() : 0);
        result = 31 * result + (sourceValue != null ? sourceValue.hashCode() : 0);
        result = 31 * result + (targetValue != null ? targetValue.hashCode() : 0);
        return result;
    }

}