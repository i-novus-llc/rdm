package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.model.Structure;
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

    private AttributeFieldDiff localizable;

    private AttributeFieldDiff description;

    private DiffStatusEnum diffStatus;

    public AttributeDiff() {
        // Nothing to do.
    }

    public AttributeDiff(Structure.Attribute oldAttr, Structure.Attribute newAttr, DiffStatusEnum diffStatus) {

        this.code = newAttr.getCode() != null ? newAttr.getCode() : oldAttr.getCode();
        this.name = new AttributeDiff.AttributeFieldDiff(oldAttr.getName(), newAttr.getName());
        this.type = new AttributeDiff.AttributeFieldDiff(oldAttr.getType(), newAttr.getType());

        this.isPrimary = new AttributeDiff.AttributeFieldDiff(oldAttr.getIsPrimary(), newAttr.getIsPrimary());
        this.localizable = new AttributeDiff.AttributeFieldDiff(oldAttr.getLocalizable(), newAttr.getLocalizable());
        this.description = new AttributeDiff.AttributeFieldDiff(oldAttr.getDescription(), newAttr.getDescription());

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

    public AttributeFieldDiff getLocalizable() {
        return localizable;
    }

    public void setLocalizable(AttributeFieldDiff localizable) {
        this.localizable = localizable;
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
                Objects.equals(localizable, that.localizable) &&
                Objects.equals(description, that.description) &&

                diffStatus == that.diffStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, type, isPrimary, localizable, description, diffStatus);
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
