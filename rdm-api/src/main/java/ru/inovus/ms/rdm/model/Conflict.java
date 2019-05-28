package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public class Conflict {

    private ConflictType conflictType;

    private List<FieldValue> primaryKeys;

    public Conflict() {
    }

    public Conflict(ConflictType conflictType, List<FieldValue> primaryKeys) {
        this.conflictType = conflictType;
        this.primaryKeys = primaryKeys;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public void setConflictType(ConflictType conflictType) {
        this.conflictType = conflictType;
    }

    public List<FieldValue> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<FieldValue> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }
}
