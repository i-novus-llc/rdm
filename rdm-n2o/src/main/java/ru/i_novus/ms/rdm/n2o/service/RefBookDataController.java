package ru.i_novus.ms.rdm.n2o.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.list.N2oInputSelect;
import net.n2oapp.framework.api.metadata.control.plain.N2oDatePicker;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.meta.control.Control;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.util.ConflictUtils;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataDecorator;
import ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils;
import ru.i_novus.ms.rdm.n2o.model.DataGridColumn;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.DateFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.EXACT;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.LIKE;

@Component
public class RefBookDataController {

    private static final String DATA_FILTER_IS_INVALID_EXCEPTION_CODE = "data.filter.is.invalid";
    private static final String DATA_FILTER_FIELD_NOT_FOUND_EXCEPTION_CODE = "data.filter.field.not.found";
    private static final String DATA_FILTER_BOOL_IS_INVALID_EXCEPTION_CODE = "data.filter.bool.is.invalid";

    private static final String BOOL_FIELD_ID = "id";
    private static final String BOOL_FIELD_NAME = "name";

    private static final String DATA_CONFLICTED_CELL_BG_COLOR = "#f8c8c6";

    private static final String BOOL_TRUE_REGEX = "true|t|y|yes|yeah|д|да|истина|правда";
    private static final Pattern BOOL_TRUE_PATTERN = Pattern.compile(BOOL_TRUE_REGEX);
    private static final String BOOL_FALSE_REGEX = "false|f|n|no|nah|н|нет|ложь|неправда";
    private static final Pattern BOOL_FALSE_PATTERN = Pattern.compile(BOOL_FALSE_REGEX);

    static final SearchDataCriteria EMPTY_SEARCH_DATA_CRITERIA = new SearchDataCriteria(0, 1);
    private static final Map<String, Object> DATA_CONFLICTED_CELL_OPTIONS = getDataConflictedCellOptions();

    @Autowired
    private DataFieldFilterProvider dataFieldFilterProvider;

    @Autowired
    private VersionRestService versionService;

    @Autowired
    private ConflictService conflictService;

    @Autowired
    private RefBookDataDecorator refBookDataDecorator;

    /**
     * Поиск записей версии справочника по критерию.
     *
     * @param criteria критерий поиска
     * @return Страница записей
     */
    @SuppressWarnings("unused") // used in: data.query.xml
    public Page<DataGridRow> getList(DataCriteria criteria) {

        RefBookVersion version = versionService.getById(criteria.getVersionId());
        Structure structure = version.getStructure();

        if (criteria.getOptLockValue() != null) {
            version.setOptLockValue(criteria.getOptLockValue());
        }

        if (criteria.getLocaleCode() != null) {
            criteria.setHasDataConflict(false);
        }

        Page<Long> conflictedRowIdsPage = null;
        if (criteria.isHasDataConflict()) {

            long conflictsCount = conflictService.countConflictedRowIds(toConflictCriteria(criteria));
            if (conflictsCount == 0)
                return new RestPage<>(emptyList(), EMPTY_SEARCH_DATA_CRITERIA, 0);

            long dataCount = versionService.search(version.getId(), EMPTY_SEARCH_DATA_CRITERIA).getTotalElements();
            if (conflictsCount != dataCount) {
                conflictedRowIdsPage = getConflictedRowIds(criteria, (int) conflictsCount);
            }
        }

        List<Long> conflictedRowIds = (conflictedRowIdsPage == null) ? emptyList() : conflictedRowIdsPage.getContent();
        SearchDataCriteria searchDataCriteria = toSearchDataCriteria(criteria, structure, conflictedRowIds);

        Page<RefBookRowValue> rowValues = searchRowValues(version.getId(), searchDataCriteria);
        List<DataGridRow> result = getDataGridContent(criteria, version, rowValues.getContent());

        long total;
        if (criteria.isHasDataConflict())
            total = (conflictedRowIdsPage == null) ? 0 : conflictedRowIdsPage.getTotalElements();
        else
            total = rowValues.getTotalElements();

        // NB: (костыль)
        // Прибавляется 1 к количеству элементов
        // из-за особенности подсчёта количества для последней страницы.
        // На клиенте отнимается 1 для всех страниц.
        return new RestPage<>(result, searchDataCriteria, total + 1);
    }

    /**
     * Поиск версии справочника.
     *
     * @param versionId идентификатор версии справочника
     * @return Версия справочника
     */
    @SuppressWarnings("unused") // used in: dataDeleteAll.query.xml
    public RefBookVersion getVersion(Integer versionId, Integer optLockValue) {

        RefBookVersion version = versionService.getById(versionId);
        if (optLockValue != null) {
            version.setOptLockValue(optLockValue);
        }

        return version;
    }

    private Page<Long> getConflictedRowIds(DataCriteria criteria, int pageSize) {

        RefBookConflictCriteria conflictCriteria = toConflictCriteria(criteria);
        conflictCriteria.setPageSize(pageSize);
        return conflictService.searchConflictedRowIds(conflictCriteria);
    }

    private RefBookConflictCriteria toConflictCriteria(DataCriteria criteria) {

        RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        conflictCriteria.setReferrerVersionId(criteria.getVersionId());
        conflictCriteria.setConflictTypes(ConflictUtils.getDataConflictTypes());
        conflictCriteria.setIsLastPublishedVersion(true);
        return conflictCriteria;
    }

    private SearchDataCriteria toSearchDataCriteria(DataCriteria criteria, Structure structure,
                                                    List<Long> conflictedRowIds) {

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize());
        searchDataCriteria.setLocaleCode(criteria.getLocaleCode());

        List<AttributeFilter> filters = toAttributeFilters(criteria, structure);
        searchDataCriteria.addAttributeFilterList(filters);

        List<Sort.Order> orders = criteria.getSorting() == null ? emptyList() : singletonList(toSortOrder(criteria.getSorting()));
        searchDataCriteria.setOrders(orders);

        if (criteria.isHasDataConflict()) {
            searchDataCriteria.setRowSystemIds(conflictedRowIds);
        }
        return searchDataCriteria;
    }

    private List<AttributeFilter> toAttributeFilters(DataCriteria criteria, Structure structure) {

        final Map<String, Serializable> filterMap = criteria.getFilter();

        if (isEmpty(filterMap))
            return new ArrayList<>();

        try {
            return filterMap.entrySet().stream()
                    .filter(e -> !ObjectUtils.isEmpty(e.getValue()))
                    .map(e -> toAttributeFilter(structure, e.getKey(), e.getValue()))
                    .filter(Objects::nonNull)
                    .collect(toList());

        } catch (Exception e) {
            throw new UserException(DATA_FILTER_IS_INVALID_EXCEPTION_CODE, e);
        }
    }

    private AttributeFilter toAttributeFilter(Structure structure, String filterName, Serializable filterValue) {

        if (filterValue == null || isEmpty(filterName))
            return null;

        String attributeCode = DataRecordUtils.deletePrefix(filterName);
        Structure.Attribute attribute = structure.getAttribute(attributeCode);
        if (attribute == null || attribute.getType() == null)
            throw new IllegalArgumentException(DATA_FILTER_FIELD_NOT_FOUND_EXCEPTION_CODE);

        Serializable attributeValue = castFilterValue(attribute, filterValue);
        if (attributeValue == null)
            return null;

        AttributeFilter attributeFilter = new AttributeFilter(attributeCode, attributeValue, attribute.getType());
        attributeFilter.setSearchType(attribute.getType() == STRING ? LIKE : EXACT);
        return attributeFilter;
    }

    private static Serializable castFilterValue(Structure.Attribute attribute, Serializable value) {

        return switch (attribute.getType()) {
            case INTEGER -> parseInteger((String) value);
            case FLOAT -> parseFloat((String) value);
            case DATE -> parseDate((String) value);
            case BOOLEAN -> parseBoolean((String) value);
            default -> value;
        };
    }

    private static BigInteger parseInteger(String value) {
        return new BigInteger(value);
    }

    private static BigDecimal parseFloat(String value) {
        return new BigDecimal(value.replace(",", ".").trim());
    }

    private static LocalDate parseDate(String value) {
        return LocalDate.parse(value, TimeUtils.DATE_TIME_PATTERN_EUROPEAN_FORMATTER);
    }

    private static Serializable parseBoolean(String value) {

        String stringValue = value.toLowerCase();
        if (isEmpty(stringValue))
            return null;

        if (BOOL_TRUE_PATTERN.matcher(stringValue).matches())
            return true;

        if (BOOL_FALSE_PATTERN.matcher(stringValue).matches())
            return false;

        throw new IllegalArgumentException(DATA_FILTER_BOOL_IS_INVALID_EXCEPTION_CODE);
    }

    private static Sort.Order toSortOrder(Sorting sorting) {
        return new Sort.Order(Direction.ASC.equals(sorting.getDirection()) ? ASC : DESC, sorting.getField());
    }

    private Page<RefBookRowValue> searchRowValues(Integer versionId, SearchDataCriteria searchDataCriteria) {

        Page<RefBookRowValue> rowValues = versionService.search(versionId, searchDataCriteria);
        if (searchDataCriteria.getLocaleCode() != null && isEmpty(rowValues.getContent())) {
            searchDataCriteria.setLocaleCode(null);
            rowValues = versionService.search(versionId, searchDataCriteria);
        }
        return rowValues;
    }

    private List<DataGridRow> getDataGridContent(DataCriteria criteria, RefBookVersion version,
                                                 List<RefBookRowValue> searchContent) {

        Structure dataStructure = refBookDataDecorator.getDataStructure(version.getId(), criteria);
        DataGridRow dataGridHead = new DataGridRow(createHead(dataStructure));

        List<RefBookRowValue> dataContent = refBookDataDecorator.getDataContent(searchContent, criteria);
        List<DataGridRow> dataGridRows = getDataGridRows(criteria, version, dataContent);

        List<DataGridRow> resultRows = new ArrayList<>();
        resultRows.add(dataGridHead);
        resultRows.addAll(dataGridRows);
        return resultRows;
    }

    private List<DataGridRow> getDataGridRows(DataCriteria criteria, RefBookVersion version,
                                              List<RefBookRowValue> searchContent) {

        List<Long> conflictedRowsIds = criteria.isHasDataConflict() || (criteria.getLocaleCode() != null)
                ? emptyList()
                : conflictService.getReferrerConflictedIds(version.getId(), getRowSystemIds(searchContent));

        return searchContent.stream()
                .map(rowValue -> {
                    boolean isDataConflict = criteria.isHasDataConflict() ||
                            conflictedRowsIds.contains(rowValue.getSystemId());
                    return toDataGridRow(rowValue, version, criteria, isDataConflict);
                })
                .collect(toList());
    }

    private List<Long> getRowSystemIds(List<RefBookRowValue> rowValues) {

        return rowValues.stream().map(RowValue::getSystemId).collect(toList());
    }

    // NB: to-do: DataGridRowCriteria ?!
    private DataGridRow toDataGridRow(RowValue<?> rowValue, RefBookVersion version,
                                      DataCriteria criteria, boolean isDataConflict) {

        Map<String, Object> rowMap = new HashMap<>();
        LongRowValue longRowValue = (LongRowValue) rowValue;

        longRowValue.getFieldValues().forEach(fieldValue ->
                rowMap.put(addPrefix(fieldValue.getField()), fieldValueToCell(fieldValue, isDataConflict))
        );

        rowMap.put(FIELD_SYSTEM_ID, String.valueOf(longRowValue.getSystemId()));
        rowMap.put(FIELD_VERSION_ID, String.valueOf(version.getId()));
        rowMap.put(FIELD_OPT_LOCK_VALUE, String.valueOf(version.getOptLockValue()));
        rowMap.put(FIELD_LOCALE_CODE, criteria.getLocaleCode());

        return new DataGridRow(longRowValue.getSystemId(), rowMap);
    }

    private static Map<String, Object> getDataConflictedCellOptions() {
        Map<String, Object> cellOptions = new HashMap<>();
        cellOptions.put("src", "TextCell");

        Map<String, Object> styleOptions = new HashMap<>();
        styleOptions.put("backgroundColor", DATA_CONFLICTED_CELL_BG_COLOR);
        cellOptions.put("styles", styleOptions);

        return cellOptions;
    }

    private Object fieldValueToCell(FieldValue<?> fieldValue, boolean isDataConflict) {

        String stringValue = fieldValueToString(fieldValue);

        if (isDataConflict)
            return new DataGridCell(stringValue, DATA_CONFLICTED_CELL_OPTIONS);

        return stringValue;
    }

    private String fieldValueToString(FieldValue<?> fieldValue) {

        Optional<Object> valueOptional = ofNullable(fieldValue).map(FieldValue::getValue);

        if (fieldValue instanceof ReferenceFieldValue) {
            return valueOptional.filter(o -> o instanceof Reference).map(o -> (Reference)o)
                    .map(RefBookDataController::referenceToString).orElse(null);

        }

        if (fieldValue instanceof DateFieldValue) {
            return valueOptional.filter(o -> o instanceof LocalDate).map(o -> (LocalDate)o)
                    .map(RefBookDataController::dateToString).orElse(null);
        }

        return valueOptional.map(String::valueOf).orElse(null);
    }

    private static String referenceToString(Reference reference) {
        return reference.getValue() != null ? reference.getDisplayValue() : null;
    }

    private static String dateToString(LocalDate localDate) {
        return localDate.format(ofPattern(TimeUtils.DATE_PATTERN_EUROPEAN));
    }

    private List<DataGridColumn> createHead(Structure structure) {

        return structure.getAttributes().stream().map(this::toDataColumn).collect(toList());
    }

    private DataGridColumn toDataColumn(Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());

        N2oField n2oField = toN2oField(attribute);
        n2oField.setId(codeWithPrefix);

        StandardField<Control> filterField = dataFieldFilterProvider.toFilterField(n2oField);

        return new DataGridColumn(codeWithPrefix, attribute.getName(),
                true, true, true, filterField.getControl());
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
                booleanField.setValueFieldId(BOOL_FIELD_ID);
                booleanField.setLabelFieldId(BOOL_FIELD_NAME);
                booleanField.setOptions(getBooleanValues());
                return booleanField;

            default:
                return new N2oInputText();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String>[] getBooleanValues() {
        return new Map[]{
                ImmutableMap.of(BOOL_FIELD_ID, "true", BOOL_FIELD_NAME, "ИСТИНА"),
                ImmutableMap.of(BOOL_FIELD_ID, "false", BOOL_FIELD_NAME, "ЛОЖЬ")
        };
    }

    @SuppressWarnings("WeakerAccess")
    public static class DataGridRow {

        @JsonProperty
        private Long id;

        @JsonProperty
        private List<DataGridColumn> columns;

        @JsonProperty
        private Map<String, Object> row;

        @SuppressWarnings("unused")
        public DataGridRow() {
            // Nothing to do.
        }

        public DataGridRow(List<DataGridColumn> columns) {
            this.columns = columns;
        }

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

    @SuppressWarnings("WeakerAccess")
    public static class DataGridCell {

        @JsonProperty
        private String value;

        @JsonProperty
        private Map<String, Object> cellOptions;

        @SuppressWarnings("unused")
        public DataGridCell() {
            // Nothing to do.
        }

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
