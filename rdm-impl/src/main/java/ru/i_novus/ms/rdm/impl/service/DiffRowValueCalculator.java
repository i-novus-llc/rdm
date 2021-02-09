package ru.i_novus.ms.rdm.impl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum.*;

public class DiffRowValueCalculator {

    private static final Logger logger = LoggerFactory.getLogger(DiffRowValueCalculator.class);

    private DiffRowValue firstDiff;
    private DiffRowValue lastDiff;
    private Set<String> commonFields;

    public DiffRowValueCalculator(DiffRowValue firstDiff, DiffRowValue lastDiff, Set<String> changedFieldNames) {
        this.firstDiff = firstDiff;
        this.lastDiff = lastDiff;
        this.commonFields = getCommonFields(firstDiff.getValues(),
                isNull(changedFieldNames) ? Set.of() : changedFieldNames);
    }

    public DiffRowValueCalculator(DiffRowValue firstDiff, DiffRowValue lastDiff) {
        this.firstDiff = firstDiff;
        this.lastDiff = lastDiff;
        this.commonFields = getCommonFields(firstDiff.getValues(), Set.of());
    }

    private Set<String> getCommonFields(List<DiffFieldValue> fieldValues, Set<String> changedFieldNames) {
        return fieldValues.stream()
                .map(DiffFieldValue::getField)
                .map(Field::getName)
                .filter(fieldName -> !changedFieldNames.contains(fieldName))
                .collect(Collectors.toSet());
    }

    public DiffRowValue calculate() {
        if (hasNoChanges()) {
            logger.debug("Differences reverted or only in changed fields. Calculate result is null. " +
                    "First diff: {}, lastDiff: {}, unchanged fields: {}",
                    firstDiff.getValues(), lastDiff, commonFields);
            return null;
        } else if (lastDiff == null) {
            return excludeUncommonFields(firstDiff);
        } else {
            return calculateDiffRowValue();
        }
    }

    private DiffRowValue excludeUncommonFields(DiffRowValue diffRowValue) {
        DiffStatusEnum status = diffRowValue.getStatus();
        List<DiffFieldValue> commonFieldsValues = diffRowValue.getValues().stream()
                .filter(fieldValue -> commonFields.contains(fieldValue.getField().getName()))
                .collect(Collectors.toList());
        return new DiffRowValue(commonFieldsValues, status);
    }

    public boolean hasNoChanges() {
        return createdAndDeleted() || allCommonFieldsValuesNotChanged();
    }

    private boolean createdAndDeleted() {
        return lastDiff != null
                && INSERTED == firstDiff.getStatus()
                && DELETED == lastDiff.getStatus();
    }

    private boolean allCommonFieldsValuesNotChanged() {
        return commonFields.stream().allMatch(fieldName -> {
            Object oldValue = getOldValue(firstDiff, fieldName);
            Object newValue = getNewValue(lastDiff == null ? firstDiff : lastDiff, fieldName);
            return changed(oldValue, newValue);
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

        if (firstDiff.getStatus() == lastDiff.getStatus()) {
            rowStatus = firstDiff.getStatus();
        } else {
            switch (firstDiff.getStatus()) {
                case INSERTED:
                    rowStatus = INSERTED;
                    break;
                case UPDATED:
                    rowStatus = lastDiff.getStatus() == DELETED ? DELETED : UPDATED;
                    break;
                case DELETED:
                    rowStatus = UPDATED;
                    break;
                default:
                    logger.error("Unexpected first diff status: ({})", firstDiff);
                    throw new IllegalArgumentException();
            }
        }
        return rowStatus;
    }

    private DiffFieldValue calculateFieldValue(String fieldName, DiffStatusEnum rowStatus) {
        Field field = getDiffFieldValue(firstDiff, fieldName).getField();
        Object oldValue = rowStatus != INSERTED ? getOldValue(firstDiff, fieldName) : null;
        Object newValue = rowStatus != DELETED ? getNewValue(lastDiff, fieldName) : null;
        DiffStatusEnum status = calculateFieldStatus(rowStatus, oldValue, newValue);

        return new DiffFieldValue<>(field, status == null ? null :  oldValue, newValue, status);
    }

    private DiffStatusEnum calculateFieldStatus(DiffStatusEnum rowStatus, Object oldValue, Object newValue) {
        return rowStatus != UPDATED || !changed(oldValue, newValue) ? rowStatus : null;
    }

    private Object getOldValue(DiffRowValue diffRowValue, String fieldName) {
        DiffFieldValue diffFieldValue = getDiffFieldValue(diffRowValue, fieldName);
        return nonNull(diffFieldValue.getStatus()) ? diffFieldValue.getOldValue() : diffFieldValue.getNewValue();
    }

    private Object getNewValue(DiffRowValue diffRowValue, String fieldName) {
        return getDiffFieldValue(diffRowValue, fieldName).getNewValue();
    }

    private DiffFieldValue getDiffFieldValue(DiffRowValue diffRowValue, String fieldName) {
        DiffFieldValue value = diffRowValue.getDiffFieldValue(fieldName);
        if (value == null) {
            logger.error("No field with name '{}' found in row: {}", fieldName, diffRowValue);
            throw new IllegalArgumentException();
        }
        return value;
    }

    private boolean changed(Object oldValue, Object newValue) {
        return (oldValue == null && newValue == null) || (oldValue != null && oldValue.equals(newValue));
    }
}
