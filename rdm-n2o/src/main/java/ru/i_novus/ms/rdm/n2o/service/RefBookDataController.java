package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.list.N2oInputSelect;
import net.n2oapp.framework.api.metadata.control.plain.N2oDatePicker;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.meta.cell.AbstractCell;
import net.n2oapp.framework.api.metadata.meta.control.Control;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import net.n2oapp.framework.api.metadata.meta.widget.table.SimpleColumn;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.util.ConflictUtils;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataDecorator;
import ru.i_novus.ms.rdm.n2o.model.grid.DataGridColumnsConfig;
import ru.i_novus.ms.rdm.n2o.model.grid.DataGridRow;
import ru.i_novus.ms.rdm.n2o.model.grid.DataGridRowType;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DateFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.*;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.*;
import static ru.i_novus.ms.rdm.n2o.util.RefBookDataUtils.castFilterValue;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.REFERENCE;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.EXACT;
import static ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum.LIKE;

@Component
public class RefBookDataController {

    private static final String DATA_BOOLEAN_VALUE_PREFIX = "data.boolean.value.";

    private static final String DATA_FILTER_IS_INVALID_EXCEPTION_CODE = "data.filter.is.invalid";
    private static final String DATA_FILTER_FIELD_NOT_FOUND_EXCEPTION_CODE = "data.filter.field.not.found";
    private static final String DATA_SORT_IS_INVALID_EXCEPTION_CODE = "data.sort.is.invalid";
    private static final String DATA_SORT_FIELD_NOT_FOUND_EXCEPTION_CODE = "data.sort.field.not.found";

    private static final SearchDataCriteria EMPTY_SEARCH_DATA_CRITERIA = new SearchDataCriteria(0, 1);
    private static final List<FieldType> LIKE_FIELD_TYPES = List.of(STRING, REFERENCE);

    private final VersionRestService versionService;

    private final ConflictService conflictService;

    private final DataFieldFilterProvider dataFieldFilterProvider;

    private final RefBookDataDecorator refBookDataDecorator;

    private final Messages messages;

    @Autowired
    public RefBookDataController(VersionRestService versionService,
                                 ConflictService conflictService,
                                 DataFieldFilterProvider dataFieldFilterProvider,
                                 RefBookDataDecorator refBookDataDecorator,
                                 Messages messages) {
        this.versionService = versionService;
        this.conflictService = conflictService;

        this.dataFieldFilterProvider = dataFieldFilterProvider;
        this.refBookDataDecorator = refBookDataDecorator;

        this.messages = messages;
    }

    /**
     * Поиск записей версии справочника по критерию.
     *
     * @param criteria критерий поиска
     * @return Страница записей
     */
    @SuppressWarnings("unused") // used in: data.query.xml
    public Page<DataGridRow> getList(DataCriteria criteria) {

        final RefBookVersion version = versionService.getById(criteria.getVersionId());
        final Structure structure = version.getStructure();

        if (criteria.getOptLockValue() != null) {
            version.setOptLockValue(criteria.getOptLockValue());
        }

        if (criteria.getLocaleCode() != null) {
            criteria.setHasDataConflict(false);
        }

        Page<Long> conflictedRowIdsPage = null;
        if (criteria.isHasDataConflict()) {

            long conflictsCount = conflictService.countConflictedRowIds(toConflictCriteria(criteria));
            if (conflictsCount == 0) {
                // Временный фикс для правильной работы кнопок на вкладке "Данные с конфликтами":
                // NB: Use getEmptyContent() only after fix buttons
                final List<DataGridRow> result = getDataGridContent(criteria, version, emptyList());
                return new RestPage<>(result, EMPTY_SEARCH_DATA_CRITERIA, 0);
            }

            long dataCount = versionService.search(version.getId(), EMPTY_SEARCH_DATA_CRITERIA).getTotalElements();
            if (conflictsCount != dataCount) {
                conflictedRowIdsPage = searchConflictedRowIds(criteria, (int) conflictsCount);
            }
        }

        final List<Long> conflictedRowIds = (conflictedRowIdsPage == null)
                ? emptyList()
                : conflictedRowIdsPage.getContent();
        final SearchDataCriteria searchDataCriteria = toSearchDataCriteria(criteria, structure, conflictedRowIds);

        final Page<RefBookRowValue> rowValues = searchRowValues(version.getId(), searchDataCriteria);
        final List<DataGridRow> result = getDataGridContent(criteria, version, rowValues.getContent());

        long total;
        if (criteria.isHasDataConflict())
            total = (conflictedRowIdsPage == null) ? 0 : conflictedRowIdsPage.getTotalElements();
        else
            total = rowValues.getTotalElements();

        return new RestPage<>(result, criteria, total);
    }

    /**
     * Поиск версии справочника.
     *
     * @param versionId идентификатор версии справочника
     * @return Версия справочника
     */
    @SuppressWarnings("unused") // used in: dataDeleteAll.query.xml
    public RefBookVersion getVersion(Integer versionId, Integer optLockValue) {

        final RefBookVersion version = versionService.getById(versionId);
        if (optLockValue != null) {
            version.setOptLockValue(optLockValue);
        }

        return version;
    }

    private Page<Long> searchConflictedRowIds(DataCriteria criteria, int pageSize) {

        final RefBookConflictCriteria conflictCriteria = toConflictCriteria(criteria);
        conflictCriteria.setPageSize(pageSize);

        return conflictService.searchConflictedRowIds(conflictCriteria);
    }

    private RefBookConflictCriteria toConflictCriteria(DataCriteria criteria) {

        final RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        conflictCriteria.setReferrerVersionId(criteria.getVersionId());
        conflictCriteria.setConflictTypes(ConflictUtils.getDataConflictTypes());
        conflictCriteria.setIsLastPublishedVersion(true);

        return conflictCriteria;
    }

    private SearchDataCriteria toSearchDataCriteria(DataCriteria criteria,
                                                    Structure structure,
                                                    List<Long> conflictedRowIds) {

        final SearchDataCriteria result = new SearchDataCriteria(criteria.getPageNumber(), criteria.getPageSize());
        result.setLocaleCode(criteria.getLocaleCode());

        final Set<List<AttributeFilter>> filters = toAttributeFilters(criteria, structure);
        result.setAttributeFilters(filters);

        final List<Sort.Order> orders = toSortOrders(criteria.getOrders(), structure);
        if (!isEmpty(orders)) {
            result.setOrders(orders);
        }

        if (criteria.isHasDataConflict()) {
            result.setRowSystemIds(conflictedRowIds);
        }

        return result;
    }

    protected Set<List<AttributeFilter>> toAttributeFilters(DataCriteria criteria, Structure structure) {

        final List<AttributeFilter> list = toAttributeFilterList(criteria, structure);
        if (isEmpty(list))
            return emptySet();

        final Set<List<AttributeFilter>> set = new HashSet<>(1);
        set.add(list);

        return set;
    }

    private List<AttributeFilter> toAttributeFilterList(DataCriteria criteria, Structure structure) {

        final Map<String, Serializable> filterMap = criteria.getFilter();
        if (isEmpty(filterMap))
            return emptyList();

        try {
            return filterMap.entrySet().stream()
                    .filter(e -> !ObjectUtils.isEmpty(e.getValue()))
                    .map(e -> toAttributeFilter(structure, e.getKey(), e.getValue()))
                    .filter(Objects::nonNull)
                    .collect(toList());

        } catch (UserException e) {
            throw e;

        } catch (Exception e) {
            throw new UserException(DATA_FILTER_IS_INVALID_EXCEPTION_CODE, e);
        }
    }

    /**
     * Преобразование фильтра из критерия поиска в фильтр по атрибуту (при наличии атрибута).
     *
     * @param structure   структура справочника
     * @param filterName  наименование фильтра
     * @param filterValue значение фильтра
     * @return Фильтр по атрибуту или null
     */
    private AttributeFilter toAttributeFilter(Structure structure, String filterName, Serializable filterValue) {

        if (filterValue == null || StringUtils.isEmpty(filterName) || !hasPrefix(filterName))
            return null;

        final String attributeCode = deletePrefix(filterName);

        final Structure.Attribute attribute = structure.getAttribute(attributeCode);
        if (attribute == null || attribute.getType() == null)
            throw new NotFoundException(new Message(DATA_FILTER_FIELD_NOT_FOUND_EXCEPTION_CODE, attributeCode, filterName));

        final Serializable attributeValue = castFilterValue(attribute, filterValue);
        if (attributeValue == null)
            return null;

        final AttributeFilter attributeFilter = new AttributeFilter(attributeCode, attributeValue, attribute.getType());
        attributeFilter.setSearchType(toSearchType(attribute));

        return attributeFilter;
    }

    private SearchTypeEnum toSearchType(Structure.Attribute attribute) {
        return LIKE_FIELD_TYPES.contains(attribute.getType()) ? LIKE : EXACT;
    }

    private List<Sort.Order> toSortOrders(List<Sort.Order> orders, Structure structure) {

        if (isEmpty(orders))
            return emptyList();

        try {
            return orders.stream().map(order -> toSortOrder(structure, order)).toList();

        } catch (UserException e) {
            throw e;

        } catch (Exception e) {
            throw new UserException(DATA_SORT_IS_INVALID_EXCEPTION_CODE, e);
        }
    }

    private Sort.Order toSortOrder(Structure structure, Sort.Order order) {

        final String orderName = order.getProperty();
        final String attributeCode = deletePrefix(orderName);

        final Structure.Attribute attribute = structure.getAttribute(attributeCode);
        if (attribute == null || attribute.getType() == null)
            throw new NotFoundException(new Message(DATA_SORT_FIELD_NOT_FOUND_EXCEPTION_CODE, attributeCode, orderName));

        return new Sort.Order(order.getDirection(), attributeCode);
    }

    private Page<RefBookRowValue> searchRowValues(Integer versionId, SearchDataCriteria searchDataCriteria) {

        Page<RefBookRowValue> rowValues = versionService.search(versionId, searchDataCriteria);
        if (searchDataCriteria.getLocaleCode() != null && isEmpty(rowValues.getContent())) {
            searchDataCriteria.setLocaleCode(null);
            rowValues = versionService.search(versionId, searchDataCriteria);
        }
        return rowValues;
    }

    private List<DataGridRow> getDataGridContent(DataCriteria criteria,
                                                 RefBookVersion version,
                                                 List<RefBookRowValue> searchContent) {

        final List<RefBookRowValue> dataContent = refBookDataDecorator.getDataContent(searchContent, criteria);
        List<DataGridRow> dataGridRows = toDataGridRows(criteria, version, dataContent);
        if (isEmpty(dataGridRows)) {
            dataGridRows = getEmptyContent();
        }

        final DataGridRow firstRow = dataGridRows.getFirst();
        final Structure dataStructure = refBookDataDecorator.getDataStructure(version.getId(), criteria);
        firstRow.setColumnsConfig(createColumnConfig(dataStructure != null ? dataStructure : Structure.EMPTY));

        return dataGridRows;
    }

    private List<DataGridRow> getEmptyContent() {

        return singletonList(new DataGridRow(null, emptyMap()));
    }

    private List<DataGridRow> toDataGridRows(DataCriteria criteria,
                                             RefBookVersion version,
                                             List<RefBookRowValue> dataContent) {

        final List<Long> conflictedRowsIds = criteria.isHasDataConflict() || (criteria.getLocaleCode() != null)
                ? emptyList()
                : conflictService.getReferrerConflictedIds(version.getId(), getRowSystemIds(dataContent));

        return dataContent.stream()
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
    private DataGridRow toDataGridRow(RowValue<?> rowValue,
                                      RefBookVersion version,
                                      DataCriteria criteria,
                                      boolean isDataConflict) {

        final Map<String, Object> rowMap = new HashMap<>(rowValue.getFieldValues().size() + 5);

        rowValue.getFieldValues().forEach(fieldValue ->
                rowMap.put(addPrefix(fieldValue.getField()), fieldValueToString(fieldValue))
        );

        final LongRowValue longRowValue = (LongRowValue) rowValue;
        final Long systemId = longRowValue.getSystemId();

        rowMap.put(FIELD_SYSTEM_ID, String.valueOf(systemId));
        rowMap.put(FIELD_ROW_TYPE, toRowType(isDataConflict).name());

        rowMap.put(FIELD_VERSION_ID, String.valueOf(version.getId()));
        rowMap.put(FIELD_OPT_LOCK_VALUE, String.valueOf(version.getOptLockValue()));
        rowMap.put(FIELD_LOCALE_CODE, criteria.getLocaleCode());

        return new DataGridRow(systemId, rowMap);
    }

    private String fieldValueToString(FieldValue<?> fieldValue) {

        final Optional<Object> valueOptional = ofNullable(fieldValue).map(FieldValue::getValue);

        if (fieldValue instanceof ReferenceFieldValue) {
            return valueOptional.filter(Reference.class::isInstance).map(o -> (Reference)o)
                    .map(RefBookDataController::referenceToString).orElse(null);

        }

        if (fieldValue instanceof DateFieldValue) {
            return valueOptional.filter(LocalDate.class::isInstance).map(o -> (LocalDate)o)
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

    private DataGridRowType toRowType(boolean isDataConflict) {
        return isDataConflict ? DataGridRowType.CONFLICTED : DataGridRowType.DEFAULT;
    }

    private DataGridColumnsConfig createColumnConfig(Structure structure) {
        return new DataGridColumnsConfig(createColumnHeaders(structure), createColumnCells(structure));
    }

    private List<SimpleColumn> createColumnHeaders(Structure structure) {
        return structure.getAttributes().stream().map(this::toColumnHeader).collect(toList());
    }

    private List<AbstractCell> createColumnCells(Structure structure) {
        return structure.getAttributes().stream().map(this::toColumnCell).collect(toList());
    }

    private SimpleColumn toColumnHeader(Structure.Attribute attribute) {

        final String id = addPrefix(attribute.getCode());

        final N2oField n2oField = toN2oField(attribute);
        n2oField.setId(FILTER_PREFIX + id);
        final StandardField<Control> filterField = dataFieldFilterProvider.toFilterField(n2oField);

        return DataGridColumnsConfig.createHeader(id, attribute.getName(), filterField);
    }

    private AbstractCell toColumnCell(Structure.Attribute attribute) {

        final String id = addPrefix(attribute.getCode());
        return DataGridColumnsConfig.createTextCell(id);
    }

    /** Преобразование атрибута в поле для поиска значений по этому атрибуту. */
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
                dateField.setDateFormat("DD.MM.YYYY"); // DATE_PATTERN_EUROPEAN
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
    private Map<String, String>[] getBooleanValues() {
        return new Map[]{
                Map.of(BOOL_FIELD_ID, "true", BOOL_FIELD_NAME, getBooleanValueName(true)),
                Map.of(BOOL_FIELD_ID, "false", BOOL_FIELD_NAME, getBooleanValueName(false))
        };
    }

    /** Наименование значения boolean. */
    private String getBooleanValueName(Boolean value) {
        return value != null ? messages.getMessage(DATA_BOOLEAN_VALUE_PREFIX + value) : null;
    }
}
