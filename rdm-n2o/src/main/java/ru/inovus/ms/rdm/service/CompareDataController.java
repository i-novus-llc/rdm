package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.compare.ComparableField;
import ru.inovus.ms.rdm.model.compare.ComparableFieldValue;
import ru.inovus.ms.rdm.model.compare.ComparableRow;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.List;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.util.ComparableUtils.*;

@Controller
public class CompareDataController {

    @Autowired
    CompareService compareService;
    @Autowired
    VersionService versionService;

    public Page<ComparableRow> getOldWithDiff(CompareCriteria criteria) {
        Structure oldStructure = versionService.getStructure(criteria.getOldVersionId());
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), null);
        Page<RowValue> oldData = versionService.search(criteria.getOldVersionId(), searchDataCriteria);

        RefBookDataDiff refBookDataDiff = getRefBookDataDiff(criteria, oldData, oldStructure);

        List<ComparableField> comparableFields = createOldVersionComparableFieldsList(refBookDataDiff, oldStructure);
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
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), null);
        Page<RowValue> newData = versionService.search(criteria.getNewVersionId(), searchDataCriteria);

        RefBookDataDiff refBookDataDiff = getRefBookDataDiff(criteria, newData, newStructure);

        List<ComparableField> comparableFields = createNewVersionComparableFieldsList(refBookDataDiff, newStructure);

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

    private RefBookDataDiff getRefBookDataDiff(CompareCriteria criteria, Page<RowValue> data, Structure structure) {
        CompareDataCriteria compareDataCriteria = new CompareDataCriteria(criteria);
        compareDataCriteria.setPrimaryAttributesFilters(createPrimaryAttributesFilters(data, structure));

        return compareService.compareData(compareDataCriteria);
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

}