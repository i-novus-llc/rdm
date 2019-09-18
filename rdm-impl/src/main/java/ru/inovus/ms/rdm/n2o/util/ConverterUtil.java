package ru.inovus.ms.rdm.n2o.util;

import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.data.domain.Sort;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.*;
import ru.i_novus.platform.versioned_data_storage.pg_impl.service.FieldFactoryImpl;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.n2o.model.version.AttributeFilter;
import ru.inovus.ms.rdm.n2o.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.Structure;

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

    private static FieldFactory fieldFactory = new FieldFactoryImpl();

    private ConverterUtil() {
    }

    public static List<Field> fields(Structure structure) {
        List<Field> fields = new ArrayList<>();
        if (structure != null) {
            Optional.ofNullable(structure.getAttributes()).ifPresent(s ->
                    s.forEach(attribute -> fields.add(field(attribute)))
            );
        }
        return fields;
    }

    public static Field field(Structure.Attribute attribute) {
        return fieldFactory.createSearchField(attribute.getCode(), attribute.getType());
    }

    public static RowValue rowValue(Row row, Structure structure) {
        List<Field> fields = ConverterUtil.fields(structure);
        return new LongRowValue(row.getSystemId(),
                fields.stream()
                        .map(field -> field.valueOf(row.getData().get(field.getName())))
                        .collect(toList()));
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

    public static Set<List<FieldSearchCriteria>> getFieldSearchCriteriaList(Set<List<AttributeFilter>> attributeFilters) {
        if (Objects.isNull(attributeFilters))
            return emptySet();
        return attributeFilters.stream().map(attrFiltersList ->
                attrFiltersList.stream().map(attrFilter ->
                        new FieldSearchCriteria(
                                fieldFactory.createField(attrFilter.getAttributeName(), attrFilter.getFieldType()),
                                attrFilter.getSearchType(),
                                singletonList(attrFilter.getValue()))).collect(toList())
        ).collect(toSet());
    }

    public static Set<List<FieldSearchCriteria>> getFieldSearchCriteriaList(Map<String, String> filters, Structure structure) {
        if (isEmpty(filters))
            return emptySet();
        return singleton(filters.entrySet().stream()
                .map(e -> {
                    Structure.Attribute attribute = structure.getAttribute(e.getKey());
                    if (attribute == null) return null;
                    Field field = field(attribute);
                    return new FieldSearchCriteria(field, SearchTypeEnum.LIKE, singletonList(toSearchType(field, e.getValue())));
                }).collect(toList()));
    }

    public static Row toRow(RowValue rowValue) {
        Map<String, Object> data = new HashMap<>();
        rowValue.getFieldValues().forEach(fieldValue -> {
            FieldValue fv = (FieldValue) fieldValue;
            data.put(fv.getField(), fv.getValue());
        });
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
        rowValue.getFieldValues().forEach(fieldValue -> map.put(fieldValue.getField(), getPlainValue(fieldValue)));
        return map;
    }

    public static Object getPlainValue(FieldValue fieldValue) {
        if (fieldValue == null) return null;
        if (fieldValue.getValue() instanceof Reference) {
            return ((Reference) fieldValue.getValue()).getValue();
        }
        return fieldValue.getValue();
    }

    public static Object castReferenceValue(Field field, String value) {
        if (field instanceof BooleanField) {
            return Boolean.valueOf(value);
        } else if (field instanceof DateField) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return LocalDate.parse(value, formatter);
        } else if (field instanceof FloatField) {
            return Float.parseFloat(value);
        } else if (field instanceof IntegerField) {
            return BigInteger.valueOf(Long.parseLong(value));
        } else if (field instanceof StringField) {
            return value;
        } else if (field instanceof TreeField) {
            return value;
        } else
            throw new RdmException("invalid field type");
    }

    public static Object toSearchType(Object value) {
        if (value instanceof Reference) {
            return ((Reference) value).getValue();
        }
        return value;
    }

    public static Object toSearchType(Field field, String value) {
        try {
            if (field instanceof BooleanField) {
                return Boolean.valueOf(value);
            } else if (field instanceof DateField) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(value, formatter);
            } else if (field instanceof FloatField) {
                return Float.parseFloat(value);
            } else if (field instanceof IntegerField) {
                return BigInteger.valueOf(Long.parseLong(value));
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new UserException("invalid.search.value", e);
        }
    }
}
