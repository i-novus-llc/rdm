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

    private AttributeFieldDiff description;

    private DiffStatusEnum diffStatus;

    public AttributeDiff() {
    }

    public AttributeDiff(String code, AttributeFieldDiff name, AttributeFieldDiff type, AttributeFieldDiff isPrimary, AttributeFieldDiff description, DiffStatusEnum diffStatus) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.isPrimary = isPrimary;
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

        if (code != null ? !code.equals(that.code) : that.code != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (isPrimary != null ? !isPrimary.equals(that.isPrimary) : that.isPrimary != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return diffStatus == that.diffStatus;

    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (isPrimary != null ? isPrimary.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (diffStatus != null ? diffStatus.hashCode() : 0);
        return result;
    }

    public static class AttributeFieldDiff {

        private Object oldValue;
        private Object newValue;

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
}
