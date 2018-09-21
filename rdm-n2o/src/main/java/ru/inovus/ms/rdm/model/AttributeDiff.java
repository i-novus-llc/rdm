package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

import java.util.Objects;

/**
 * Created by znurgaliev on 20.09.2018.
 */
public class AttributeDiff {

    private String code;

    private AttributeFieldDiff name;

    private AttributeFieldDiff type;

    private AttributeFieldDiff isPrimary;

    private AttributeFieldDiff isRequired;

    private AttributeFieldDiff description;

    private DiffStatusEnum diffStatus;

    public AttributeDiff() {
    }

    public AttributeDiff(String code, AttributeFieldDiff name, AttributeFieldDiff type, AttributeFieldDiff isPrimary, AttributeFieldDiff isRequired, AttributeFieldDiff description, DiffStatusEnum diffStatus) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.isPrimary = isPrimary;
        this.isRequired = isRequired;
        this.description = description;
        this.diffStatus = diffStatus;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public AttributeFieldDiff getName() {
        return name;
    }

    public void setName(AttributeFieldDiff name) {
        this.name = name;
    }

    public AttributeFieldDiff getType() {
        return type;
    }

    public void setType(AttributeFieldDiff type) {
        this.type = type;
    }

    public AttributeFieldDiff getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(AttributeFieldDiff isPrimary) {
        this.isPrimary = isPrimary;
    }

    public AttributeFieldDiff getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(AttributeFieldDiff isRequired) {
        this.isRequired = isRequired;
    }

    public AttributeFieldDiff getDescription() {
        return description;
    }

    public void setDescription(AttributeFieldDiff description) {
        this.description = description;
    }

    public DiffStatusEnum getDiffStatus() {
        return diffStatus;
    }

    public void setDiffStatus(DiffStatusEnum diffStatus) {
        this.diffStatus = diffStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeDiff that = (AttributeDiff) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(isPrimary, that.isPrimary) &&
                Objects.equals(isRequired, that.isRequired) &&
                Objects.equals(description, that.description) &&
                Objects.equals(diffStatus, that.diffStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, type, isPrimary, isRequired, description, diffStatus);
    }

    public static class AttributeFieldDiff {

        @Override
        public String toString() {
            return "AttributeFieldDiff{" +
                    "oldValue=" + oldValue +
                    ", newValue=" + newValue +
                    '}';
        }

        Object oldValue;
        Object newValue;

        public AttributeFieldDiff() {
        }

        public AttributeFieldDiff(Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
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
            AttributeFieldDiff that = (AttributeFieldDiff) o;
            return Objects.equals(oldValue, that.oldValue) &&
                    Objects.equals(newValue, that.newValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(oldValue, newValue);
        }
    }

    @Override
    public String toString() {
        return "AttributeDiff{" +
                "code='" + code + '\'' +
                ", name=" + name +
                ", type=" + type +
                ", isPrimary=" + isPrimary +
                ", isRequired=" + isRequired +
                ", description=" + description +
                ", diffStatus=" + diffStatus +
                '}';
    }
}
