package ru.inovus.ms.rdm.model;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public class Conflict {

    private String refAttributeCode;

    private ConflictType conflictType;

    private List<FieldValue> primaryValues;

    public Conflict() {
    }

    public Conflict(String refAttributeCode, ConflictType conflictType, List<FieldValue> primaryValues) {
        this.refAttributeCode = refAttributeCode;
        this.conflictType = conflictType;
        this.primaryValues = primaryValues;
    }

    public Conflict(ConflictType conflictType, List<FieldValue> primaryValues) {
        this.conflictType = conflictType;
        this.primaryValues = primaryValues;
    }

    public String getRefAttributeCode() {
        return refAttributeCode;
    }

    public void setRefAttributeCode(String refAttributeCode) {
        this.refAttributeCode = refAttributeCode;
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

    public boolean isEmpty() {
        return StringUtils.isEmpty(refAttributeCode) || CollectionUtils.isEmpty(primaryValues);
    }
}
