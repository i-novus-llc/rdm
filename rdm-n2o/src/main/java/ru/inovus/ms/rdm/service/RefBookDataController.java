package ru.inovus.ms.rdm.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.framework.api.MetadataEnvironment;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.list.N2oInputSelect;
import net.n2oapp.framework.api.metadata.control.plain.N2oDatePicker;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import net.n2oapp.framework.api.metadata.pipeline.CompilePipeline;
import net.n2oapp.framework.config.compile.pipeline.N2oPipelineSupport;
import net.n2oapp.framework.config.metadata.compile.context.WidgetContext;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.DateFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.criteria.DataCriteria;
import ru.inovus.ms.rdm.model.DataGridColumn;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.model.version.AttributeFilter;
import ru.inovus.ms.rdm.provider.N2oDomain;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static com.google.common.collect.ImmutableMap.of;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static ru.inovus.ms.rdm.RdmUiUtil.addPrefix;
import static ru.inovus.ms.rdm.RdmUiUtil.deletePrefix;
import static ru.inovus.ms.rdm.util.TimeUtils.DATE_PATTERN_EUROPEAN;
import static ru.inovus.ms.rdm.util.TimeUtils.DATE_TIME_PATTERN_EUROPEAN_FORMATTER;

@Component
public class RefBookDataController {

    private static final String BOOL_FIELD_ID = "id";
    private static final String BOOL_FIELD_NAME = "name";

    private static Map<String, Object> cellOptions = getConflictedCellOptions();

    @Autowired
    private MetadataEnvironment env;
    @Autowired
    private VersionService versionService;
    @Autowired
    private ConflictService conflictService;

    public Page<DataGridRow> getList(DataCriteria criteria) {

        Structure structure = versionService.getStructure(criteria.getVersionId());

        Page<Long> conflictedRowIdsPage = null;
        if (BooleanUtils.isTrue(criteria.getHasConflict())) {

            long conflictsCount = conflictService.countConflictedRowIds(toRefBookConflictCriteria(criteria));
            long dataCount = versionService.search(criteria.getVersionId(), new SearchDataCriteria()).getTotalElements();
            if (conflictsCount != dataCount) {
                conflictedRowIdsPage = getConflictedRowIds(criteria, (int) conflictsCount);
            }
        }

        List<Long> conflictedRowIds = (conflictedRowIdsPage == null) ? emptyList() : conflictedRowIdsPage.getContent();
        SearchDataCriteria searchDataCriteria = toSearchDataCriteria(criteria, structure, conflictedRowIds);

        Page<RefBookRowValue> search = versionService.search(criteria.getVersionId(), searchDataCriteria);
        List<DataGridRow> result = getDataGridContent(criteria, search.getContent(), structure, BooleanUtils.isTrue(criteria.getHasConflict()));

        long total;
        if (BooleanUtils.isTrue(criteria.getHasConflict()))
            total = (conflictedRowIdsPage == null) ? 0 : conflictedRowIdsPage.getTotalElements();
        else
            total = search.getTotalElements();

        // NB: (костыль)
        // Прибавляется 1 к количеству элементов
        // из-за особенности подсчёта количества для последней страницы.
        // На клиенте отнимается 1 для всех страниц.
        return new RestPage<>(result, searchDataCriteria, total + 1);
    }

    private Page<Long> getConflictedRowIds(DataCriteria criteria, int pageSize) {
        RefBookConflictCriteria refBookConflictCriteria = toRefBookConflictCriteria(criteria);
        refBookConflictCriteria.setPageSize(pageSize);
        return conflictService.searchConflictedRowIds(refBookConflictCriteria);
    }

    private RefBookConflictCriteria toRefBookConflictCriteria(DataCriteria criteria) {
        RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        conflictCriteria.setPageSize(criteria.getSize());
        conflictCriteria.setReferrerVersionId(criteria.getVersionId());
        conflictCriteria.setIsLastPublishedVersion(true);
        return conflictCriteria;
    }

    private SearchDataCriteria toSearchDataCriteria(DataCriteria criteria, Structure structure, List<Long> conflictedRowIds) {
        List<AttributeFilter> filters = new ArrayList<>();
        if (criteria.getFilter() != null) {
            try {
                criteria.getFilter().forEach((k, v) -> {
                    if (v == null) return;
                    String attributeCode = deletePrefix(k);
                    Structure.Attribute attribute = structure.getAttribute(attributeCode);
                    if (attribute == null)
                        throw new IllegalArgumentException("Filter field not found");

                    filters.add(new AttributeFilter(attributeCode, castValue(attribute, v), attribute.getType()));
                });
            } catch (Exception e) {
                throw new UserException("invalid.filter.exception", e);
            }
        }

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize(), singleton(filters));
        List<Sort.Order> orders = ofNullable(criteria.getSorting())
                .map(sorting -> new Sort.Order(Direction.ASC.equals(sorting.getDirection()) ? ASC : DESC, sorting.getField()))
                .map(Collections::singletonList).orElse(emptyList());
        searchDataCriteria.setOrders(orders);

        if (BooleanUtils.isTrue(criteria.getHasConflict())) {
            searchDataCriteria.setRowSystemIds(conflictedRowIds);
        }
        return searchDataCriteria;
    }

    private Serializable castValue(Structure.Attribute attribute, Serializable value) {
        if (attribute == null || value == null)
            return null;

        switch (attribute.getType()) {
            case INTEGER:
                return new BigInteger((String) value);

            case FLOAT:
                return new BigDecimal(((String) value).replace(",", ".").trim());

            case DATE:
                return LocalDate.parse((String) value, DATE_TIME_PATTERN_EUROPEAN_FORMATTER);

            case BOOLEAN:
                return Boolean.valueOf((String) ((Map) value).get(BOOL_FIELD_ID));

            default:
                return value;
        }
    }

    private List<DataGridRow> getDataGridContent(DataCriteria criteria, List<RefBookRowValue> search, Structure structure, boolean allWithConflicts) {
        DataGridRow dataGridHead = new DataGridRow(createHead(structure));
        List<DataGridRow> dataGridRows = getDataGridRows(criteria, search, allWithConflicts);

        List<DataGridRow> resultRows = new ArrayList<>();
        resultRows.add(dataGridHead);
        resultRows.addAll(dataGridRows);
        return resultRows;
    }

    private List<DataGridRow> getDataGridRows(DataCriteria criteria, List<RefBookRowValue> search, boolean allWithConflicts) {
        List<Long> conflictedRowsIds = allWithConflicts
                ? emptyList()
                : conflictService.getReferrerConflictedIds(criteria.getVersionId(), getRowSystemIds(search));

        return search.stream()
                .map(rowValue -> {
                            boolean rowHasConflict = allWithConflicts || conflictedRowsIds.contains(rowValue.getSystemId());

                            return toDataGridRow(
                                    rowValue,
                                    criteria.getVersionId(),
                                    rowHasConflict
                            );
                        }
                )
                .collect(toList());
    }

    private List<Long> getRowSystemIds(List<RefBookRowValue> rowValues) {
        return rowValues.stream()
                .map(RowValue::getSystemId)
                .collect(toList());
    }

    private DataGridRow toDataGridRow(RowValue rowValue, Integer versionId, boolean hasConflict) {
        Map<String, Object> row = new HashMap<>();
        LongRowValue longRowValue = (LongRowValue) rowValue;
        longRowValue.getFieldValues()
                .forEach(fieldValue -> {
                            Object cell = hasConflict
                                    ? new DataGridCell(toStringValue(fieldValue), cellOptions)
                                    : toStringValue(fieldValue);
                            row.put(addPrefix(fieldValue.getField()), cell);
                        }
                );
        row.put("id", String.valueOf(longRowValue.getSystemId()));
        row.put("versionId", String.valueOf(versionId));
        return new DataGridRow(longRowValue.getSystemId(), row);
    }

    private static Map<String, Object> getConflictedCellOptions () {
        Map<String, Object> cellOptions = new HashMap<>();
        cellOptions.put("src", "TextCell");

        Map<String, Object> styleOptions = new HashMap<>();
        styleOptions.put("backgroundColor", "#f8c8c6");
        cellOptions.put("styles", styleOptions);

        return cellOptions;
    }

    private String toStringValue(FieldValue value) {
        Optional<Object> valueOptional = ofNullable(value).map(FieldValue::getValue);
        if (value instanceof ReferenceFieldValue)
            return valueOptional.filter(o -> o instanceof Reference).map(o -> (Reference) o)
                    .map(this::referenceToString).orElse(null);

        else if (value instanceof DateFieldValue)
            return valueOptional.filter(o -> o instanceof LocalDate).map(o -> (LocalDate) o)
                    .map(localDate -> localDate.format(ofPattern(DATE_PATTERN_EUROPEAN)))
                    .orElse(null);

        return valueOptional.map(String::valueOf).orElse(null);
    }

    private String referenceToString(Reference reference) {
        return reference.getValue() != null
                ? reference.getValue() + ": " + reference.getDisplayValue()
                : null;
    }

    private List<DataGridColumn> createHead(Structure structure) {
        return structure.getAttributes().stream()
                .map(this::toDataColumn)
                .collect(toList());
    }

    private DataGridColumn toDataColumn(Structure.Attribute attribute) {
        N2oField n2oField = toN2oField(attribute);
        n2oField.setId(addPrefix(attribute.getCode()));
        CompilePipeline pipeline = N2oPipelineSupport.compilePipeline(env);
        CompileContext<?, ?> ctx = new WidgetContext("");
        StandardField field = pipeline.compile().get(n2oField, ctx);

        return new DataGridColumn(addPrefix(attribute.getCode()), attribute.getName(),
                true, true, true,
                field.getControl());
    }

    private N2oField toN2oField(Structure.Attribute attribute) {

        switch (attribute.getType()) {
            case INTEGER:
                N2oInputText integerField = new N2oInputText();
                integerField.setDomain(N2oDomain.INTEGER);
                integerField.setStep("1");
                return integerField;

            case FLOAT:
                N2oInputText floatField = new N2oInputText();
                floatField.setDomain(N2oDomain.FLOAT);
                floatField.setStep("0.0001");
                return floatField;

            case DATE:
                N2oDatePicker dateField = new N2oDatePicker();
                dateField.setDateFormat("DD.MM.YYYY");
                return dateField;

            case BOOLEAN:
                N2oInputSelect booleanField = new N2oInputSelect();
                booleanField.setValueFieldId("id");
                booleanField.setLabelFieldId("name");
                booleanField.setOptions(getBooleanValues());
                return booleanField;

            default:
                return new N2oInputText();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String>[] getBooleanValues() {
        return new Map[]{
                of(BOOL_FIELD_ID, "true", BOOL_FIELD_NAME, "ИСТИНА"),
                of(BOOL_FIELD_ID, "false", BOOL_FIELD_NAME, "ЛОЖЬ")};
    }

    public static class DataGridRow {

        @JsonProperty
        Long id;
        @JsonProperty
        List<DataGridColumn> columns;
        @JsonProperty
        Map<String, Object> row;

        @SuppressWarnings("unused")
        public DataGridRow() {
        }

        @SuppressWarnings("WeakerAccess")
        public DataGridRow(List<DataGridColumn> columns) {
            this.columns = columns;
        }

        @SuppressWarnings("WeakerAccess")
        public DataGridRow(Long id, Map<String, Object> row) {
            this.id = id;
            this.row = row;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<DataGridColumn> getColumns() {
            return columns;
        }

        public void setColumns(List<DataGridColumn> columns) {
            this.columns = columns;
        }

        public Map<String, Object> getRow() {
            return row;
        }

        public void setRow(Map<String, Object> row) {
            this.row = row;
        }
    }

    public static class DataGridCell {
        @JsonProperty
        String value;
        @JsonProperty
        Map<String, Object> cellOptions;

        @SuppressWarnings("unused")
        public DataGridCell() {
        }

        @SuppressWarnings("WeakerAccess")
        public DataGridCell(String value, Map<String, Object> cellOptions) {
            this.value = value;
            this.cellOptions = cellOptions;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Map<String, Object> getCellOptions() {
            return cellOptions;
        }

        @SuppressWarnings("unused")
        public void setCellOptions(Map<String, Object> cellOptions) {
            this.cellOptions = cellOptions;
        }
    }
}
