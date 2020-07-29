package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableFieldValue;
import ru.i_novus.ms.rdm.api.model.compare.ComparableRow;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.util.ComparableUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.i_novus.ms.rdm.api.util.ComparableUtils.createPrimaryAttributesFilters;

@Controller
@SuppressWarnings("WeakerAccess")
public class CompareDataController {

    @Autowired
    CompareService compareService;
    @Autowired
    VersionService versionService;

    public Page<ComparableRow> getOldWithDiff(CompareDataCriteria criteria) {
        return getComparableRowsPage(criteria.getOldVersionId(), criteria, DiffStatusEnum.DELETED);
    }

    public Page<ComparableRow> getNewWithDiff(CompareDataCriteria criteria) {
        return getComparableRowsPage(criteria.getNewVersionId(), criteria, DiffStatusEnum.INSERTED);
    }

    private Page<ComparableRow> getComparableRowsPage(Integer versionId, CompareDataCriteria criteria, DiffStatusEnum status) {
        Structure structure = versionService.getStructure(versionId);
        Page<RefBookRowValue> data = versionService.search(versionId, new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize(), null));

        criteria.setPrimaryAttributesFilters(ComparableUtils.createPrimaryAttributesFilters(data, structure));
        Page<ComparableRow> commonComparableRows = compareService.getCommonComparableRows(criteria);
        List<ComparableRow> resultComparableRows = new ArrayList<>();
        data.getContent().forEach(rowValue -> {
            ComparableRow comparableRow = ComparableUtils.findComparableRow(structure.getPrimary(), rowValue, commonComparableRows.getContent(), status);
            ComparableRow resultComparableRow = new ComparableRow(
                    structure
                            .getAttributes()
                            .stream()
                            .map(attribute -> getComparableFieldValue(comparableRow, attribute, status))
                            .collect(Collectors.toList()),
                    comparableRow.getStatus());
            resultComparableRows.add(resultComparableRow);
        });
        return new PageImpl<>(resultComparableRows, criteria, data.getTotalElements());
    }

    /*
     * Вернет значение ComparableField для конкретного атрибута структуры.
     * Если вызван для старой версии (статус DiffStatusEnum.DELETED) и атрибут был изменен (статус атрибута UPDATED),
     * то название атрибута будет взято из старой структуры.
     * Для полей новой версии атрибут не изменится.
     *
     * @param comparableRow     строка, из которой получаем значение
     * @param attribute         атрибут структуры, для которого получаем значение
     * @param status            старая (DELETED) или новая (INSERTED) версия
     * @return Значение
     */
    private ComparableFieldValue getComparableFieldValue(ComparableRow comparableRow, Structure.Attribute attribute, DiffStatusEnum status) {
        ComparableFieldValue cfv = comparableRow.getComparableFieldValue(attribute.getCode());
        if (DiffStatusEnum.DELETED.equals(status) &&
                DiffStatusEnum.UPDATED.equals(cfv.getComparableField().getStatus()))
            cfv.getComparableField().setName(attribute.getName());
        return cfv;
    }

}