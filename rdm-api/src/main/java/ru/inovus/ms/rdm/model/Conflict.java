package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public class Conflict {

    private ConflictType conflictType;

    private List<FieldValue> primaryValues;

    public Conflict() {
    }

    public Conflict(ConflictType conflictType, List<FieldValue> primaryValues) {
        this.conflictType = conflictType;
        this.primaryValues = primaryValues;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public void setConflictType(ConflictType conflictType) {
        this.conflictType = conflictType;
    }

    public List<FieldValue> getPrimaryValues() {
        return primaryValues;
    }

    public void setPrimaryValues(List<FieldValue> primaryValues) {
        this.primaryValues = primaryValues;
    }

}
