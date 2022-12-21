package ru.i_novus.ms.rdm.impl.service.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.*;

public class DiffRowValueCalculator {

    private static final Logger logger = LoggerFactory.getLogger(DiffRowValueCalculator.class);

    private final DiffRowValue firstDiff;
    private final DiffRowValue lastDiff;
    private final Set<String> commonFields;
    private final boolean isBackward;

    public DiffRowValueCalculator(DiffRowValue firstDiff,
                                  DiffRowValue lastDiff,
                                  Set<String> changedFieldNames,
                                  boolean isBackward) {
        this.firstDiff = firstDiff;
        this.lastDiff = lastDiff;
        this.commonFields = getCommonFields(firstDiff.getValues(), changedFieldNames);
        this.isBackward = isBackward;
    }

    public DiffRowValueCalculator(DiffRowValue firstDiff,
                                  DiffRowValue lastDiff,
                                  Set<String> changedFieldNames) {
        this.firstDiff = firstDiff;
        this.lastDiff = lastDiff;
        this.commonFields = getCommonFields(firstDiff.getValues(), changedFieldNames);
        this.isBackward = false;
    }

    private Set<String> getCommonFields(List<DiffFieldValue> fieldValues, Set<String> changedFieldNames) {
        Set<String> excludedFields = isNull(changedFieldNames) ? emptySet() : changedFieldNames;
        return fieldValues.stream()
                .map(DiffFieldValue::getField)
                .map(Field::getName)
                .filter(fieldName -> !excludedFields.contains(fieldName))
                .collect(toSet());
    }

    public DiffRowValue calculate() {
        if (isAnnihilated()) {
            logger.debug("Differences reverted or only in changed fields. Calculate result is null. " +
                            "First diff: {}, lastDiff: {}, unchanged fields: {}",
                    firstDiff.getValues(), lastDiff, commonFields);
            return null;
        } else {
            return calculateDiffRowValue();
        }
    }

    public boolean isAnnihilated() {
        return noChangesInCommonFields();
    }

    private boolean noChangesInCommonFields() {
        return commonFields.stream().allMatch(fieldName -> {
            Object oldValue = getOldValue(fieldName);
            Object newValue = getNewValue(fieldName);
            return valueEquals(oldValue, newValue);
        });
    }

    private DiffRowValue calculateDiffRowValue() {
        List<DiffFieldValue> fieldValues = new ArrayList<>();
        DiffStatusEnum rowStatus = calculateRowStatus();

        for (String fieldName : commonFields)
            fieldValues.add(calculateFieldValue(fieldName, rowStatus));

        return new DiffRowValue(fieldValues, rowStatus);
    }

    private DiffStatusEnum calculateRowStatus() {
        DiffStatusEnum rowStatus;
        if (!isBackward) {
            DiffStatusEnum firstDiffStatus = firstDiff.getStatus();
            DiffStatusEnum lastDiffStatus = isNull(lastDiff) ? null : lastDiff.getStatus();
            rowStatus = calculateRowStatus(firstDiffStatus, lastDiffStatus);
        } else {
            DiffStatusEnum firstDiffStatus = isNull(lastDiff) ? firstDiff.getStatus() : lastDiff.getStatus();
            DiffStatusEnum lastDiffStatus = isNull(lastDiff) ? null : firstDiff.getStatus();
            rowStatus = getReverseStatus(calculateRowStatus(firstDiffStatus, lastDiffStatus));
        }
        return rowStatus;
    }

    private DiffStatusEnum calculateRowStatus(DiffStatusEnum firstDiffStatus, DiffStatusEnum lastDiffStatus) {

        if (lastDiffStatus == null || firstDiffStatus == lastDiffStatus) {
            return firstDiffStatus;
        }

        switch (firstDiffStatus) {
            case INSERTED:
                return INSERTED;
            case UPDATED:
                return lastDiffStatus == DELETED ? DELETED : UPDATED;
            case DELETED:
                return UPDATED;
            default:
                String errorMessage = String.format("Unexpected first diff status: %s", firstDiffStatus);
                logger.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
        }
    }

    private static DiffStatusEnum getReverseStatus(DiffStatusEnum status) {
        if (status == INSERTED)
            return DELETED;
        else if (status == DELETED)
            return INSERTED;
        else
            return status;
    }

    private DiffFieldValue calculateFieldValue(String fieldName, DiffStatusEnum rowStatus) {
        Field field = getDiffFieldValue(firstDiff, fieldName).getField();
        Serializable oldValue = rowStatus == INSERTED ? null : getOldValue(fieldName);
        Serializable newValue = rowStatus == DELETED ? null : getNewValue(fieldName);
        DiffStatusEnum status = calculateFieldStatus(rowStatus, oldValue, newValue);

        return new DiffFieldValue<>(field, status == null ? null : oldValue, newValue, status);
    }

    private DiffStatusEnum calculateFieldStatus(DiffStatusEnum rowStatus, Object oldValue, Object newValue) {
        return rowStatus != UPDATED || !valueEquals(oldValue, newValue) ? rowStatus : null;
    }

    private Serializable getOldValue(String fieldName) {
        DiffFieldValue diffFieldValue = getDiffFieldValue(firstDiff, fieldName);
        if (isBackward)
            return diffFieldValue.getNewValue();
        else if (nonNull(diffFieldValue.getStatus()))
            return diffFieldValue.getOldValue();
        else
            return diffFieldValue.getNewValue();
    }

    private Serializable getNewValue(String fieldName) {
        DiffFieldValue diffFieldValue = getDiffFieldValue(lastDiff == null ? firstDiff : lastDiff, fieldName);
        if (!isBackward)
            return diffFieldValue.getNewValue();
        else if (nonNull(diffFieldValue.getStatus()))
            return diffFieldValue.getOldValue();
        else
            return diffFieldValue.getNewValue();
    }

    private DiffFieldValue getDiffFieldValue(DiffRowValue diffRowValue, String fieldName) {
        DiffFieldValue value = diffRowValue.getDiffFieldValue(fieldName);
        if (value == null) {
            String errorMessage = String.format("No field with name '%1$s' found in row: %2$s", fieldName, diffRowValue);
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private boolean valueEquals(Object oldValue, Object newValue) {
        return (oldValue == null && newValue == null) ||
                (oldValue != null && oldValue.equals(newValue));
    }
}
