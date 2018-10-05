package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Controller
public class CompareDataController {

    @Autowired
    CompareService compareService;
    @Autowired
    VersionService versionService;

    public Page<ComparableRow> getOldWithDiff(CompareCriteria criteria) {
        Structure oldStructure = versionService.getStructure(criteria.getOldVersionId());
        SearchDataCriteria searchDataCriteria = getSearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), null);
        Page<RowValue> oldData = versionService.search(criteria.getOldVersionId(), searchDataCriteria);

        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(criteria);
        compareDataCriteria.setPrimaryFieldsFilters(getPrimaryFieldsFilters(oldData, oldStructure));
        RefBookDataDiff refBookDataDiff = compareService.compareData(compareDataCriteria);

        List<ComparableField> comparableFields = getComparableFieldsList(oldStructure, refBookDataDiff.getUpdatedAttributes(),
                refBookDataDiff.getOldAttributes(), DiffStatusEnum.DELETED);
        sortComparableList(comparableFields, refBookDataDiff.getOldAttributes().size());

        List<ComparableRow> comparableRows = oldData.getContent()
                .stream()
                .map(oldRowValue -> {
                    ComparableRow comparableRow = new ComparableRow();
                    DiffRowValue diffRowValue = getDiffRowValue(oldStructure.getPrimary(), oldRowValue,
                            refBookDataDiff.getRows().getContent());
                    comparableRow.setStatus(diffRowValue != null ? diffRowValue.getStatus() : null);
                    comparableRow.setFieldValues(comparableFields
                            .stream()
                            .map(comparableField -> {
                                ComparableFieldValue value = new ComparableFieldValue();
                                value.setComparableField(comparableField);
                                value.setOldValue(oldRowValue.getFieldValue(comparableField.getCode()).getValue());
                                value.setNewValue(getValueForComparableFieldValue(diffRowValue, comparableField,
                                        DiffStatusEnum.DELETED, value.getOldValue()));
                                return value;
                            }).collect(Collectors.toList()));
                    return comparableRow;
                }).collect(Collectors.toList());

        return new RestPage<>(comparableRows, criteria, oldData.getTotalElements());
    }

    public Page<ComparableRow> getNewWithDiff(CompareCriteria criteria) {
        Structure newStructure = versionService.getStructure(criteria.getNewVersionId());
        SearchDataCriteria searchDataCriteria = getSearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), null);
        Page<RowValue> newData = versionService.search(criteria.getNewVersionId(), searchDataCriteria);

        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(criteria);
        compareDataCriteria.setPrimaryFieldsFilters(getPrimaryFieldsFilters(newData, newStructure));
        RefBookDataDiff refBookDataDiff = compareService.compareData(compareDataCriteria);

        List<ComparableField> comparableFields = getComparableFieldsList(newStructure, refBookDataDiff.getUpdatedAttributes(),
                refBookDataDiff.getNewAttributes(), DiffStatusEnum.INSERTED);

        List<ComparableRow> comparableRows = newData.getContent()
                .stream()
                .map(newRowValue -> {
                    ComparableRow comparableRow = new ComparableRow();
                    DiffRowValue diffRowValue = getDiffRowValue(newStructure.getPrimary(), newRowValue,
                            refBookDataDiff.getRows().getContent());
                    comparableRow.setStatus(diffRowValue != null ? diffRowValue.getStatus() : null);
                    comparableRow.setFieldValues(comparableFields
                            .stream()
                            .map(comparableField -> {
                                ComparableFieldValue value = new ComparableFieldValue();
                                value.setComparableField(comparableField);
                                value.setNewValue(newRowValue.getFieldValue(comparableField.getCode()).getValue());
                                value.setOldValue(getValueForComparableFieldValue(diffRowValue, comparableField,
                                        DiffStatusEnum.INSERTED, value.getNewValue()));
                                return value;
                            }).collect(Collectors.toList()));
                    return comparableRow;
                }).collect(Collectors.toList());

        return new RestPage<>(comparableRows, criteria, newData.getTotalElements());
    }

    public Page<ComparableRow> getCommonDataDiff(CompareCriteria criteria) {

        Structure newStructure = versionService.getStructure(criteria.getNewVersionId());
        Structure oldStructure = versionService.getStructure(criteria.getOldVersionId());

        SearchDataCriteria searchDataCriteria = getSearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), null);
        Page<RowValue> newData = versionService.search(criteria.getNewVersionId(), searchDataCriteria);

        Set<List<FieldValue>> primaryFieldsFilters = getPrimaryFieldsFilters(newData, newStructure);

        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(criteria);
        compareDataCriteria.setPrimaryFieldsFilters(primaryFieldsFilters);
        RefBookDataDiff refBookDataDiff = compareService.compareData(compareDataCriteria);

        List<ComparableField> comparableFields = getComparableFieldsList(newStructure, refBookDataDiff.getUpdatedAttributes(),
                refBookDataDiff.getNewAttributes(), DiffStatusEnum.INSERTED);
        refBookDataDiff.getOldAttributes()
                .forEach(oldAttribute ->
                        comparableFields.add(
                                new ComparableField(oldAttribute, oldStructure.getAttribute(oldAttribute).getName(),
                                        DiffStatusEnum.DELETED))
                );

        List<ComparableRow> comparableRows = new ArrayList<>();

        addNewVersionRows(comparableRows, refBookDataDiff, newData, criteria, newStructure, comparableFields, primaryFieldsFilters);
        addDeletedRows(comparableRows, criteria, comparableFields, (int) newData.getTotalElements());

        return new RestPage<>(comparableRows, criteria, newData.getTotalElements() + getTotalDeletedCount(criteria));
    }

    private void addNewVersionRows(List<ComparableRow> comparableRows, RefBookDataDiff refBookDataDiff, Page<RowValue> newData,
                                   CompareCriteria criteria, Structure newStructure,
                                   List<ComparableField> comparableFields, Set<List<FieldValue>> primaryFieldsFilters) {
        if (isEmpty(newData.getContent()))
            return;

        Boolean hasUpdOrDelAttr = !isEmpty(refBookDataDiff.getUpdatedAttributes()) || !isEmpty(refBookDataDiff.getOldAttributes());

        SearchDataCriteria oldSearchDataCriteria = hasUpdOrDelAttr
                ? getSearchDataCriteria(0, criteria.getPageSize(), primaryFieldsFilters)
                : null;

        Page<RowValue> oldData = hasUpdOrDelAttr
                ? versionService.search(criteria.getOldVersionId(), oldSearchDataCriteria)
                : null;

//        add rows in order of new version
        newData.getContent()
                .forEach(newRowValue -> {
                    ComparableRow comparableRow = new ComparableRow();
                    DiffRowValue diffRowValue = getDiffRowValue(newStructure.getPrimary(), newRowValue,
                            refBookDataDiff.getRows().getContent());
                    RowValue oldRowValue = oldData != null
                            ? getRowValue(newStructure.getPrimary(), newRowValue, oldData.getContent())
                            : null;

                    comparableRow.setStatus(diffRowValue != null ? diffRowValue.getStatus() : null);
                    comparableRow.setFieldValues(comparableFields
                            .stream()
                            .map(comparableField -> {
                                ComparableFieldValue value = new ComparableFieldValue();
                                value.setComparableField(comparableField);
                                setOldAndNewValuesForComparableFieldValue(value,
                                        diffRowValue != null
                                                ? diffRowValue.getDiffFieldValue(comparableField.getCode())
                                                : null,
                                        oldRowValue,
                                        newRowValue);
                                return value;
                            }).collect(Collectors.toList()));
                    comparableRows.add(comparableRow);
                });
    }

    private void addDeletedRows(List<ComparableRow> comparableRows, CompareCriteria criteria,
                                List<ComparableField> comparableFields, int totalNewCount) {
        if (comparableRows.size() < criteria.getPageSize()) {
            int skipPageCount = criteria.getPageNumber() - totalNewCount / criteria.getPageSize();
            long newDataOnLastPageCount = totalNewCount % criteria.getPageSize();
            long skipDeletedRowsCount = criteria.getPageSize() * skipPageCount - newDataOnLastPageCount;
            long pageSize = skipDeletedRowsCount + criteria.getPageSize();
            SearchDataCriteria delSearchDataCriteria = getSearchDataCriteria(0, (int) pageSize, null);
            Page<RowValue> delData = versionService.search(criteria.getOldVersionId(), delSearchDataCriteria);
            delData.getContent()
                    .stream()
                    .skip(skipDeletedRowsCount > 0 ? skipDeletedRowsCount : 0)
                    .forEach(deletedRowValue -> {
                        ComparableRow comparableRow = new ComparableRow();
                        comparableRow.setStatus(DiffStatusEnum.DELETED);
                        comparableRow.setFieldValues(comparableFields
                                .stream()
                                .map(comparableField -> {
                                    ComparableFieldValue value = new ComparableFieldValue();
                                    value.setComparableField(comparableField);
                                    setOldAndNewValuesForComparableFieldValue(value,
                                            null,
                                            deletedRowValue,
                                            null);
                                    return value;
                                }).collect(Collectors.toList()));
                        comparableRows.add(comparableRow);
                    });
        }
    }

    private SearchDataCriteria getSearchDataCriteria(int pageNumber, int pageSize, Set<List<FieldValue>> primaryFieldsFilters) {
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setPageNumber(pageNumber);
        searchDataCriteria.setPageSize(pageSize);
        searchDataCriteria.setPrimaryFieldsFilters(primaryFieldsFilters);
        return searchDataCriteria;
    }

    private Object getValueForComparableFieldValue(DiffRowValue diffRowValue, ComparableField comparableField,
                                                   DiffStatusEnum status, Object value) {
        if (diffRowValue == null)
            return status.equals(comparableField.getStatus())
                    ? null
                    : value;
        DiffFieldValue diffFieldValue = diffRowValue.getDiffFieldValue(comparableField.getCode());
        if (diffFieldValue != null)
            return DiffStatusEnum.DELETED.equals(status) || diffFieldValue.getStatus() == null
                    ? diffFieldValue.getNewValue()
                    : diffFieldValue.getOldValue();
        return null;
    }

    private void setOldAndNewValuesForComparableFieldValue(ComparableFieldValue comparableFieldValue,
                                                           DiffFieldValue diffFieldValue,
                                                           RowValue oldRowValue, RowValue newRowValue) {
        String fieldCode = comparableFieldValue.getComparableField().getCode();
        if (comparableFieldValue.getComparableField().getStatus() == null) {
            if (diffFieldValue != null) {
                comparableFieldValue.setNewValue(diffFieldValue.getNewValue());
                comparableFieldValue.setOldValue(
                        diffFieldValue.getStatus() != null
                                ? diffFieldValue.getOldValue()
                                : diffFieldValue.getNewValue());
            } else {
                comparableFieldValue.setOldValue(getValueFromRowValue(fieldCode, oldRowValue));
                comparableFieldValue.setNewValue(getValueFromRowValue(fieldCode, newRowValue));
            }
            return;
        }
        switch (comparableFieldValue.getComparableField().getStatus()) {
            case DELETED:
                comparableFieldValue.setOldValue(getValueFromRowValue(fieldCode, oldRowValue));
                comparableFieldValue.setNewValue(null);
                return;
            case UPDATED:
                comparableFieldValue.setOldValue(getValueFromRowValue(fieldCode, oldRowValue));
                comparableFieldValue.setNewValue(getValueFromRowValue(fieldCode, newRowValue));
                return;
            case INSERTED:
                comparableFieldValue.setOldValue(null);
                comparableFieldValue.setNewValue(getValueFromRowValue(fieldCode, newRowValue));
        }
    }

    private Object getValueFromRowValue(String fieldCode, RowValue rowValue) {
        return rowValue != null
                ? rowValue.getFieldValue(fieldCode).getValue()
                : null;
    }

    private long getTotalDeletedCount(CompareCriteria criteria) {
        CompareDataCriteria deletedCountCriteria = new CompareDataCriteria(criteria);
        deletedCountCriteria.setDiffStatus(DiffStatusEnum.DELETED);
        deletedCountCriteria.setPrimaryFieldsFilters(emptySet());
        deletedCountCriteria.setCountOnly(true);
        RefBookDataDiff refBookDeletedRows = compareService.compareData(deletedCountCriteria);
        return refBookDeletedRows.getRows().getTotalElements();
    }

    private List<ComparableField> getComparableFieldsList(Structure structure, List<String> commonAttributes,
                                                          List<String> versionAttributes, DiffStatusEnum status) {
        return structure.getAttributes().stream().map(attribute -> {
            DiffStatusEnum fieldStatus = null;
            if (commonAttributes.contains(attribute.getCode()))
                fieldStatus = DiffStatusEnum.UPDATED;
            if (versionAttributes.contains(attribute.getCode()))
                fieldStatus = status;
            return new ComparableField(attribute.getCode(), attribute.getName(), fieldStatus);
        }).collect(Collectors.toList());
    }

    private Set<List<FieldValue>> getPrimaryFieldsFilters(Page<RowValue> data, Structure structure) {
        Set<List<FieldValue>> primaryFieldsFilters = new HashSet<>();
        data.forEach(row ->
                primaryFieldsFilters.add(structure.getPrimary()
                        .stream()
                        .map(pk -> row.getFieldValue(pk.getCode()))
                        .collect(Collectors.toList()))
        );
        return primaryFieldsFilters;
    }

    private <T extends RdmComparable> void sortComparableList(List<T> comparables, int count) {
        for (int i = 0; i < comparables.size() && count > 0; i++) {
            if (DiffStatusEnum.DELETED.equals(comparables.get(i).getStatus())) {
                T comparable = comparables.get(i);
                comparables.remove(i);
                comparables.add(comparable);
                count--;
            }
        }
    }

    private DiffRowValue getDiffRowValue(List<Structure.Attribute> primaries, RowValue rowValue,
                                         List<DiffRowValue> diffRowValues) {
        return diffRowValues
                .stream()
                .filter(diffRow ->
                        primaries.stream().allMatch(primary -> {
                            DiffFieldValue diffFieldValue = diffRow.getDiffFieldValue(primary.getCode());
                            return diffFieldValue != null &&
                                    rowValue.getFieldValue(primary.getCode()).getValue()
                                            .equals(
                                                    DiffStatusEnum.DELETED.equals(diffFieldValue.getStatus())
                                                            ? diffFieldValue.getOldValue()
                                                            : diffFieldValue.getNewValue()
                                            );
                        })
                )
                .findFirst()
                .orElse(null);
    }

    private RowValue getRowValue(List<Structure.Attribute> primaries, RowValue rowValue,
                                 List<RowValue> rowValues) {
        return rowValues
                .stream()
                .filter(rowValue1 ->
                        primaries.stream().allMatch(primary -> {
                            FieldValue fieldValue = rowValue.getFieldValue(primary.getCode());
                            FieldValue fieldValue1 = rowValue1.getFieldValue(primary.getCode());
                            return fieldValue != null
                                    && fieldValue1 != null
                                    && fieldValue.getValue() != null
                                    && fieldValue.getValue().equals(fieldValue1.getValue());
                        })
                )
                .findFirst()
                .orElse(null);
    }

}