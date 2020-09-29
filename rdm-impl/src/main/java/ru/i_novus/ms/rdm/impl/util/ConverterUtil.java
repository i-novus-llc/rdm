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

import java.io.Serializable;
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

@SuppressWarnings({"rawtypes", "java:S3740"})
public class ConverterUtil {

    private static final FieldFactory fieldFactory = new FieldFactoryImpl();

    private ConverterUtil() {
        throw new UnsupportedOperationException();
    }

    /** Получение списка полей на основе структуры справочника. */
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

    /** Получение записи из plain-записи на основе структуры. */
    public static RowValue rowValue(Row row, Structure structure) {

        if (structure == null || structure.isEmpty())
            return new LongRowValue(row.getSystemId(), emptyList());

        List<Field> fields = fields(structure);

        final Map<String, Object> data = row.getData();
        List<FieldValue> fieldValues = fields.stream().map(field -> toFieldValue(data, field)).collect(toList());

        return new LongRowValue(row.getSystemId(), fieldValues);
    }

    /** Формирование структуры из имеющейся структуры по данным plain-записи. */
    public static Structure toDataStructure(Structure structure, Map<String, Object> data) {

        List<Structure.Attribute> attributes = structure.getAttributes().stream()
                    .filter(attribute -> data.containsKey(attribute.getCode()))
                    .collect(toList());
        if (attributes.size() == structure.getAttributes().size())
            return structure;

        List<Structure.Reference> references = structure.getReferences();
        if (references.isEmpty())
            return new Structure(attributes, null);

        List<String> attributeCodes = attributes.stream().map(Structure.Attribute::getCode).collect(toList());
        references = structure.getReferences().stream()
                .filter(reference -> attributeCodes.contains(reference.getAttribute()))
                .collect(toList());

        return new Structure(attributes, references);
    }

    /** Получение значения поля на основе данных plain-записи справочника и самого поля. */
    @SuppressWarnings("unchecked")
    private static FieldValue toFieldValue(Map<String, Object> data, Field field) {

        return field.valueOf(data.get(field.getName()));
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

        return attributeFilters.stream()
                .map(ConverterUtil::toFieldSearchCriterias)
                .filter(list -> !isEmpty(list))
                .collect(toSet());
    }

    public static List<FieldSearchCriteria> toFieldSearchCriterias(List<AttributeFilter> attributeFilterList) {

        if (isEmpty(attributeFilterList))
            return emptyList();

        return attributeFilterList.stream().map(ConverterUtil::toFieldSearchCriteria).collect(toList());
    }

    private static FieldSearchCriteria toFieldSearchCriteria(AttributeFilter filter) {

        Field field = fieldFactory.createField(filter.getAttributeName(), filter.getFieldType());
        return new FieldSearchCriteria(field, filter.getSearchType(), singletonList(filter.getValue()));
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
        return new FieldSearchCriteria(field, SearchTypeEnum.LIKE,
                singletonList(toSearchValue(field, filter.getValue()))
        );
    }

    public static Row toRow(RowValue rowValue) {

        @SuppressWarnings("unchecked")
        List<FieldValue> fieldValues = rowValue.getFieldValues();
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

    public static Serializable castReferenceValue(Field field, String value) {

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

    public static Serializable toSearchValue(Serializable value) {

        if (value instanceof Reference) {
            return ((Reference) value).getValue();
        }

        return value;
    }

    public static Serializable toSearchValue(Field field, String value) {
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
