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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.DateFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.criteria.DataCriteria;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.of;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static ru.inovus.ms.rdm.util.TimeUtils.DATE_PATTERN_WITH_POINT;
import static ru.inovus.ms.rdm.util.TimeUtils.DATE_TIME_PATTERN_FORMATTER;


@Component
public class RefBookDataController {

    public static final String BOOL_FIELD_ID = "id";
    public static final String BOOL_FIELD_NAME = "name";


    @Autowired
    private MetadataEnvironment env;
    @Autowired
    private VersionService versionService;

    public Page<DataGridRow> getList(DataCriteria criteria) {

        Structure structure = versionService.getStructure(criteria.getVersionId());
        SearchDataCriteria searchDataCriteria = toSearchDataCriteria(criteria, structure);
        Page<RefBookRowValue> search = versionService.search(criteria.getVersionId(), searchDataCriteria);
        DataGridRow dataGridHead = new DataGridRow(createHead(structure));
        List<DataGridRow> dataGridRows = search.getContent().stream()
                .map(rowValue -> toDataGridRow(rowValue, criteria.getVersionId()))
                .collect(Collectors.toList());

        List<DataGridRow> result = new ArrayList<>();
        result.add(dataGridHead);
        result.addAll(dataGridRows);

        //прибавляется к количеству элементов 1(костыль), изза особенности подсчета количества для последней страницы
        //на клиенте отнимается 1 для всех страниц
        return new RestPage<>(result, searchDataCriteria, search.getTotalElements() + 1);
    }

    private DataGridRow toDataGridRow(RowValue rowValue, Integer versionId) {
        Map<String, String> row = new HashMap<>();
        LongRowValue longRow = (LongRowValue) rowValue;
        longRow.getFieldValues().forEach(fieldValue -> row.put(fieldValue.getField(), toStringValue(fieldValue)));
        row.put("id", String.valueOf(longRow.getSystemId()));
        row.put("versionId", String.valueOf(versionId));
        return new DataGridRow(longRow.getSystemId(), row);
    }

    private String toStringValue(FieldValue value) {
        Optional<Object> valueOptional = ofNullable(value).map(FieldValue::getValue);
        if (value instanceof ReferenceFieldValue)
            return valueOptional.filter(o -> o instanceof Reference).map(o -> (Reference) o)
                    .map(Reference::getValue).orElse(null);
        else if (value instanceof DateFieldValue)
            return valueOptional.filter(o -> o instanceof LocalDate).map(o -> (LocalDate) o)
                    .map(localDate -> localDate.format(ofPattern(DATE_PATTERN_WITH_POINT)))
                    .orElse(null);
        return valueOptional.map(String::valueOf).orElse(null);
    }

    private List<DataColumn> createHead(Structure structure) {
        return structure.getAttributes().stream()
                .map(this::toDataColumn)
                .collect(Collectors.toList());
    }

    private DataColumn toDataColumn(Structure.Attribute attribute) {
        N2oField n2oField = toN2oField(attribute);
        n2oField.setId(attribute.getCode());
        CompilePipeline pipeline = N2oPipelineSupport.compilePipeline(env);
        CompileContext<?, ?> ctx = new WidgetContext("");
        StandardField field = pipeline.compile().get(n2oField, ctx);

        return new DataColumn(attribute.getCode(), attribute.getName(), true, true, true, field.getControl());
    }

    private N2oField toN2oField(Structure.Attribute attribute) {
        if (FieldType.BOOLEAN.equals(attribute.getType())) {
            N2oInputSelect n2oInputSelect = new N2oInputSelect();
            n2oInputSelect.setValueFieldId("id");
            n2oInputSelect.setLabelFieldId("name");
            n2oInputSelect.setOptions(new Map[]{
                    of(BOOL_FIELD_ID, "true", BOOL_FIELD_NAME, "ИСТИНА"),
                    of(BOOL_FIELD_ID, "false", BOOL_FIELD_NAME, "ЛОЖЬ")});
            return n2oInputSelect;
        } else if (FieldType.DATE.equals(attribute.getType())) {
            N2oDatePicker n2oDatePicker = new N2oDatePicker();
            n2oDatePicker.setDateFormat("DD.MM.YYYY");
            return n2oDatePicker;
        } else if (FieldType.INTEGER.equals(attribute.getType())) {
            N2oInputText n2oInputText = new N2oInputText();
            n2oInputText.setDomain("integer");
            n2oInputText.setStep("1");
            return n2oInputText;
        } else if (FieldType.FLOAT.equals(attribute.getType())) {
            N2oInputText n2oInputText = new N2oInputText();
            n2oInputText.setDomain("numeric");
            n2oInputText.setStep("0.0001");
            return n2oInputText;
        } else return new N2oInputText();
    }

    private SearchDataCriteria toSearchDataCriteria(DataCriteria criteria, Structure structure) {
        List<AttributeFilter> filters = new ArrayList<>();
        if (criteria.getFilter() != null)
            try {
                criteria.getFilter().forEach((k, v) -> {
                    if (v == null) return;
                    Structure.Attribute attribute = structure.getAttribute(k);
                    if (attribute == null)
                        throw new IllegalArgumentException("Filter field not found");
                    switch (attribute.getType()) {
                        case INTEGER:
                            v = new BigInteger((String) v);
                            break;
                        case FLOAT:
                            v = new BigDecimal(((String) v).replace(",", ".").trim());
                            break;
                        case DATE:
                            v = LocalDate.parse((String) v, DATE_TIME_PATTERN_FORMATTER);
                            break;
                        case BOOLEAN:
                            v = Boolean.valueOf((String) ((Map) v).get(BOOL_FIELD_ID));
                            break;
                        default:
                            break;
                    }
                    filters.add(new AttributeFilter(k, v, structure.getAttribute(k).getType()));
                });
            } catch (Exception e) {
                throw new UserException("invalid.filter.exception", e);
            }

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(criteria.getPage() - 1, criteria.getSize(), singleton(filters));
        List<Sort.Order> orders = ofNullable(criteria.getSorting())
                .map(sorting -> new Sort.Order(Direction.ASC.equals(sorting.getDirection()) ? ASC : DESC, sorting.getField()))
                .map(Collections::singletonList).orElse(emptyList());
        searchDataCriteria.setOrders(orders);
        return searchDataCriteria;
    }

    public static class DataGridRow {

        @JsonProperty
        Long id;
        @JsonProperty
        List<DataColumn> columns;
        @JsonProperty
        Map<String, String> row;

        public DataGridRow() {
        }

        public DataGridRow(List<DataColumn> columns) {
            this.columns = columns;
        }

        public DataGridRow(Long id, Map<String, String> row) {
            this.id = id;
            this.row = row;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<DataColumn> getColumns() {
            return columns;
        }

        public void setColumns(List<DataColumn> columns) {
            this.columns = columns;
        }

        public Map<String, String> getRow() {
            return row;
        }

        public void setRow(Map<String, String> row) {
            this.row = row;
        }
    }
}
