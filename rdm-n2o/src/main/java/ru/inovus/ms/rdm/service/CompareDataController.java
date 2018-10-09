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

        RefBookDataDiff refBookDataDiff = getRefBookDataDiff(criteria, oldData, oldStructure);

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
                                value.setNewValue(getNewComparableFieldValueForOldWithDiff(diffRowValue,
                                        comparableField, value.getOldValue()));
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

        RefBookDataDiff refBookDataDiff = getRefBookDataDiff(criteria, newData, newStructure);

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
                                value.setOldValue(getOldComparableFieldValueForNewWithDiff(diffRowValue,
                                        comparableField, value.getNewValue()));
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

        RefBookDataDiff refBookDataDiff = getRefBookDataDiff(criteria, newData, newStructure);

        List<ComparableField> comparableFields = getComparableFieldsList(newStructure, refBookDataDiff.getUpdatedAttributes(),
                refBookDataDiff.getNewAttributes(), DiffStatusEnum.INSERTED);
        refBookDataDiff.getOldAttributes()
                .forEach(oldAttribute ->
                        comparableFields.add(
                                new ComparableField(oldAttribute, oldStructure.getAttribute(oldAttribute).getName(),
                                        DiffStatusEnum.DELETED))
                );

        List<ComparableRow> comparableRows = new ArrayList<>();

        Set<List<AttributeFilter>> primaryAttributesFilters = createPrimaryAttributesFilters(newData, newStructure);
        addNewVersionRows(comparableRows, refBookDataDiff, newData, criteria, newStructure, comparableFields, primaryAttributesFilters);
        addDeletedRows(comparableRows, criteria, comparableFields, (int) newData.getTotalElements());

        return new RestPage<>(comparableRows, criteria, newData.getTotalElements() + getTotalDeletedCount(criteria));
    }

    private RefBookDataDiff getRefBookDataDiff(CompareCriteria criteria, Page<RowValue> data, Structure structure) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(criteria);
        compareDataCriteria.setPrimaryAttributesFilters(createPrimaryAttributesFilters(data, structure));

        return compareService.compareData(compareDataCriteria);
    }

    private void addNewVersionRows(List<ComparableRow> comparableRows, RefBookDataDiff refBookDataDiff, Page<RowValue> newData,
                                   CompareCriteria criteria, Structure newStructure,
                                   List<ComparableField> comparableFields, Set<List<AttributeFilter>> primaryAttributesFilters) {
        if (isEmpty(newData.getContent()))
            return;

        Boolean hasUpdOrDelAttr = !isEmpty(refBookDataDiff.getUpdatedAttributes()) || !isEmpty(refBookDataDiff.getOldAttributes());

        SearchDataCriteria oldSearchDataCriteria = hasUpdOrDelAttr
                ? getSearchDataCriteria(0, criteria.getPageSize(), primaryAttributesFilters)
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
                            ? findRowValue(newStructure.getPrimary(), newRowValue, oldData.getContent())
                            : null;

                    comparableRow.setStatus(diffRowValue != null ? diffRowValue.getStatus() : null);
                    comparableRow.setFieldValues(
                            comparableFields
                                    .stream()
                                    .map(comparableField ->
                                            createComparableFieldValue(comparableField,
                                                    diffRowValue != null
                                                            ? diffRowValue.getDiffFieldValue(comparableField.getCode())
                                                            : null,
                                                    oldRowValue,
                                                    newRowValue))
                                    .collect(Collectors.toList())
                    );
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
                        comparableRow.setFieldValues(
                                comparableFields
                                        .stream()
                                        .map(comparableField ->
                                                createComparableFieldValue(comparableField,
                                                        null,
                                                        deletedRowValue,
                                                        null))
                                        .collect(Collectors.toList())
                        );
                        comparableRows.add(comparableRow);
                    });
        }
    }

    private SearchDataCriteria getSearchDataCriteria(int pageNumber, int pageSize, Set<List<AttributeFilter>> fieldsFilters) {
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setPageNumber(pageNumber);
        searchDataCriteria.setPageSize(pageSize);
        searchDataCriteria.setAttributeFilter(fieldsFilters);
        return searchDataCriteria;
    }

    /**
     * Вычисляет старое значение для атрибута структуры новой версии, которое равно:
     * - значению по умолчанию (новое значение атрибута), если нет изменений для строки и поле не было удалено
     * - старому значению из diffRowValue, если оно не пустое
     * - null, если поле было добавлено в структуру
     *
     * @param diffRowValue список первичных атрибутов для идентификации записи
     * @param comparableField атрибут, для которого вычисляется старое значение
     * @param defaultValue значение по умолчанию
     * @return Старое значение либо null
     */
    private Object getOldComparableFieldValueForNewWithDiff(DiffRowValue diffRowValue, ComparableField comparableField,
                                                            Object defaultValue) {
        if (diffRowValue == null)
            return DiffStatusEnum.INSERTED.equals(comparableField.getStatus())
                    ? null
                    : defaultValue;
        DiffFieldValue diffFieldValue = diffRowValue.getDiffFieldValue(comparableField.getCode());
        if (diffFieldValue != null)
            return diffFieldValue.getStatus() == null
                    ? diffFieldValue.getNewValue()
                    : diffFieldValue.getOldValue();
        return null;
    }

    /**
     * Вычисляет новое значение для атрибута структуры старой версии, которое равно:
     * - значению по умолчанию (старое значение атрибута), если нет изменений для строки и поле не было удалено
     * - новому значению из diffRowValue, если оно не пустое
     * - null, если поле было удалено из структуры
     *
     * @param diffRowValue список первичных атрибутов для идентификации записи
     * @param comparableField атрибут, для которого вычисляется новое значение
     * @param defaultValue значение по умолчанию
     * @return Новое значение либо null
     */
    private Object getNewComparableFieldValueForOldWithDiff(DiffRowValue diffRowValue, ComparableField comparableField,
                                                            Object defaultValue) {
        if (diffRowValue == null)
            return DiffStatusEnum.DELETED.equals(comparableField.getStatus())
                    ? null
                    : defaultValue;
        DiffFieldValue diffFieldValue = diffRowValue.getDiffFieldValue(comparableField.getCode());
        if (diffFieldValue != null)
            return diffFieldValue.getNewValue();
        return null;
    }

    private ComparableFieldValue createComparableFieldValue(ComparableField comparableField,
                                                            DiffFieldValue diffFieldValue,
                                                            RowValue oldRowValue, RowValue newRowValue) {
        String fieldCode = comparableField.getCode();

        ComparableFieldValue comparableFieldValue = new ComparableFieldValue();
        comparableFieldValue.setComparableField(comparableField);

        if (comparableField.getStatus() == null) {
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
        } else {
            switch (comparableField.getStatus()) {
                case DELETED:
                    comparableFieldValue.setOldValue(getValueFromRowValue(fieldCode, oldRowValue));
                    comparableFieldValue.setNewValue(null);
                    break;
                case UPDATED:
                    comparableFieldValue.setOldValue(getValueFromRowValue(fieldCode, oldRowValue));
                    comparableFieldValue.setNewValue(getValueFromRowValue(fieldCode, newRowValue));
                    break;
                case INSERTED:
                    comparableFieldValue.setOldValue(null);
                    comparableFieldValue.setNewValue(getValueFromRowValue(fieldCode, newRowValue));
            }
        }
        return comparableFieldValue;
    }

    private Object getValueFromRowValue(String fieldCode, RowValue rowValue) {
        return rowValue != null
                ? rowValue.getFieldValue(fieldCode).getValue()
                : null;
    }

    private long getTotalDeletedCount(CompareCriteria criteria) {
        CompareDataCriteria deletedCountCriteria = new CompareDataCriteria(criteria);
        deletedCountCriteria.setDiffStatus(DiffStatusEnum.DELETED);
        deletedCountCriteria.setPrimaryAttributesFilters(emptySet());
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

    private Set<List<AttributeFilter>> createPrimaryAttributesFilters(Page<RowValue> data, Structure structure) {
        Set<List<AttributeFilter>> primaryFieldsFilters = new HashSet<>();
        data.forEach(row ->
                primaryFieldsFilters.add(structure.getPrimary()
                        .stream()
                        .map(pk ->
                                new AttributeFilter(pk.getCode(), row.getFieldValue(pk.getCode()).getValue(), pk.getType())
                        )
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

    /**
     * В списке записей #rowValues ищется строка, которая соответствует строке #rowValue
     * на основании набора первичных ключей primaries
     *
     * @param primaries список первичных атрибутов для идентификации записи
     * @param rowValue  запись, для которой ведется поиск соответствующей в полученном списке записей
     * @param rowValues список записей, среди которых ведется поиск
     * @return Найденная запись либо null
     */
    private RowValue findRowValue(List<Structure.Attribute> primaries, RowValue rowValue,
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