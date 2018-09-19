package ru.inovus.ms.rdm.model;

import java.util.List;


public class StructureDiff {


    List<AttributeDiff> inserted;
    List<AttributeDiff> updated;
    List<AttributeDiff> deleted;

    public StructureDiff() {
    }

    public StructureDiff(List<AttributeDiff> inserted, List<AttributeDiff> updated, List<AttributeDiff> deleted) {
        this.inserted = inserted;
        this.updated = updated;
        this.deleted = deleted;
    }

    public List<AttributeDiff> getInserted() {
        return inserted;
    }

    public void setInserted(List<AttributeDiff> inserted) {
        this.inserted = inserted;
    }

    public List<AttributeDiff> getUpdated() {
        return updated;
    }

    public void setUpdated(List<AttributeDiff> updated) {
        this.updated = updated;
    }

    public List<AttributeDiff> getDeleted() {
        return deleted;
    }

    public void setDeleted(List<AttributeDiff> deleted) {
        this.deleted = deleted;
    }

    public static class AttributeDiff{
        Structure.Attribute oldAttribute;
        Structure.Attribute newAttribute;

        public AttributeDiff(Structure.Attribute oldAttribute, Structure.Attribute newAttribute) {
            this.oldAttribute = oldAttribute;
            this.newAttribute = newAttribute;
        }

        public Structure.Attribute getOldAttribute() {
            return oldAttribute;
        }

        public void setOldAttribute(Structure.Attribute oldAttribute) {
            this.oldAttribute = oldAttribute;
        }

        public Structure.Attribute getNewAttribute() {
            return newAttribute;
        }

        public void setNewAttribute(Structure.Attribute newAttribute) {
            this.newAttribute = newAttribute;
        }
    }
}
