package ru.i_novus.ms.rdm.impl.util;

import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.*;
import ru.i_novus.platform.versioned_data_storage.pg_impl.service.FieldFactoryImpl;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

public class ConverterUtil {

    private static final FieldFactory fieldFactory = new FieldFactoryImpl();

    private ConverterUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Получение списка полей на основе структуры справочника.
     */
    public static List<Field> fields(Structure structure) {

        List<Field> fields = new ArrayList<>();
        if (structure != null && !structure.isEmpty()) {
            structure.getAttributes().forEach(attribute -> fields.add(field(attribute)));
        }
        return fields;
    }

    /** Получение поля на основе атрибута структуры справочника. */
    public static Field field(Structure.Attribute attribute) {
        boolean isSearchable = attribute.hasIsPrimary() && FieldType.STRING.equals(attribute.getType());
        return isSearchable
                ? fieldFactory.createSearchField(attribute.getCode(), attribute.getType())
                : fieldFactory.createField(attribute.getCode(), attribute.getType());
    }

    public static RowValue rowValue(Row row, Structure structure) {

        List<Field> fields = fields(structure);
        return new LongRowValue(row.getSystemId(),
                fields.stream().map(field -> toFieldValue(row, field)).collect(toList())
        );
    }

    /**
     * Получение значения поля на основе записи справочника и самого поля.
     */
    private static FieldValue toFieldValue(Row row, Field field) {

        return field.valueOf(row.getData().get(field.getName()));
    }

    public static Date date(LocalDateTime date) {
        return date != null ? Date.from(date.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    public static List<Sorting> sortings(Sort sort) {

        List<Sorting> sortings = new ArrayList<>();
        for (Sort.Order order : sort) {
            sortings.add(new Sorting(order.getProperty(), Direction.valueOf(order.getDirection().name())));
        }
        return sortings;
    }

    public static Set<List<FieldSearchCriteria>> toFieldSearchCriterias(Set<List<AttributeFilter>> attributeFilters) {

        if (isEmpty(attributeFilters))
            return emptySet();

        return attributeFilters.stream().map(attrFilterList ->
                attrFilterList.stream().map(attrFilter ->
                        new FieldSearchCriteria(
                                fieldFactory.createField(attrFilter.getAttributeName(), attrFilter.getFieldType()),
                                attrFilter.getSearchType(),
                                singletonList(attrFilter.getValue()))).collect(toList())
        ).collect(toSet());
    }

    public static Set<List<FieldSearchCriteria>> toFieldSearchCriterias(Map<String, String> filters, Structure structure) {

        if (isEmpty(filters))
            return emptySet();

        return singleton(filters.entrySet().stream()
                .map(filter -> toFieldSearchCriteria(filter, structure))
                .filter(Objects::nonNull)
                .collect(toList()));
    }

    private static FieldSearchCriteria toFieldSearchCriteria(Map.Entry<String, String> filter, Structure structure) {

        Structure.Attribute attribute = structure.getAttribute(filter.getKey());
        if (attribute == null) return null;

        Field field = field(attribute);
        return new FieldSearchCriteria(field, SearchTypeEnum.LIKE, singletonList(toSearchValue(field, filter.getValue())));
    }

    public static Row toRow(RowValue rowValue) {

        List<FieldValue> fieldValues = (List<FieldValue>) rowValue.getFieldValues();
        Map<String, Object> data = new HashMap<>();
        fieldValues.forEach(fieldValue -> data.put(fieldValue.getField(), fieldValue.getValue()));

        return new Row(rowValue.getSystemId() != null
                ? Long.valueOf(String.valueOf(rowValue.getSystemId()))
                : null, data);
    }

    public static String toString(Object value) {

        if (value instanceof LocalDate) {
            return TimeUtils.format((LocalDate) value);
        }

        if (value instanceof Reference) {
            return ((Reference) value).getValue();
        }

        return String.valueOf(value);
    }

    public static Map<String, Object> toStringObjectMap(RefBookRowValue rowValue) {

        Map<String, Object> map = new HashMap<>();
        map.put("rowId", rowValue.getId());

        rowValue.getFieldValues().forEach(fieldValue -> map.put(fieldValue.getField(), toPlainValue(fieldValue)));
        return map;
    }

    public static Object toPlainValue(FieldValue fieldValue) {

        if (fieldValue == null) return null;

        if (fieldValue.getValue() instanceof Reference) {
            return ((Reference) fieldValue.getValue()).getValue();
        }

        return fieldValue.getValue();
    }

    public static Object castReferenceValue(Field field, String value) {

        if (field instanceof BooleanField) {
            return Boolean.valueOf(value);
        }

        if (field instanceof DateField) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return LocalDate.parse(value, formatter);
        }

        if (field instanceof FloatField) {
            return Float.parseFloat(value);
        }

        if (field instanceof IntegerField) {
            return BigInteger.valueOf(Long.parseLong(value));
        }

        if (field instanceof StringField) {
            return value;
        }

        if (field instanceof TreeField) {
            return value;
        }

        throw new RdmException("invalid field type");
    }

    public static Object toSearchValue(Object value) {

        if (value instanceof Reference) {
            return ((Reference) value).getValue();
        }

        return value;
    }

    public static Object toSearchValue(Field field, String value) {
        try {
            if (field instanceof BooleanField) {
                return Boolean.valueOf(value);
            }

            if (field instanceof DateField) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(value, formatter);
            }

            if (field instanceof FloatField) {
                return Float.parseFloat(value);
            }

            if (field instanceof IntegerField) {
                return BigInteger.valueOf(Long.parseLong(value));
            }

            return value;

        } catch (Exception e) {
            throw new UserException("invalid.search.value", e);
        }
    }
}
