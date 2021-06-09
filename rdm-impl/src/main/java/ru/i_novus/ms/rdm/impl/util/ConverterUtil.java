package ru.i_novus.ms.rdm.impl.util;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import net.n2oapp.platform.i18n.UserException;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.criteria.BaseDataCriteria;
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
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.DATE_PATTERN_ERA_FORMATTER;

@SuppressWarnings({"rawtypes", "java:S3740"})
public class ConverterUtil {

    private static final List<? extends Serializable> NOT_NULL_VALUES = List.of(0L);

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

    /**
     * Получение поля на основе атрибута структуры справочника.
     *
     * @param attribute атрибут
     * @return Поле
     */
    public static Field field(Structure.Attribute attribute) {

        boolean isSearchable = attribute.hasIsPrimary() && FieldType.STRING.equals(attribute.getType());
        return isSearchable
                ? fieldFactory.createSearchField(attribute.getCode(), attribute.getType())
                : field(attribute.getCode(), attribute.getType());
    }

    /**
     * Получение поля на основе атрибута-ссылки структуры справочника.
     *
     * @param reference атрибут-ссылка
     * @return Поле
     */
    public static Field field(Structure.Reference reference) {

        return field(reference.getAttribute(), FieldType.REFERENCE);
    }

    /**
     * Получение поля по коду и типу атрибута.
     *
     * @param code код атрибута = наименование поля
     * @param type тип атрибута
     * @return Поле
     */
    public static Field field(String code, FieldType type) {

        return fieldFactory.createField(code, type);
    }

    /** Получение записи из plain-записи на основе структуры. */
    public static RowValue rowValue(Row row, Structure structure) {

        Map<String, Object> data = row.getData();
        List<Field> fields = fields(structure);
        List<FieldValue> fieldValues = fields.stream().map(field -> toFieldValue(data, field)).collect(toList());

        return new LongRowValue(row.getSystemId(), fieldValues);
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

    /**
     * Преобразование набора фильтров по атрибуту в набор критериев поиска по полю.
     *
     * @param filters набор фильтров по атрибуту
     * @return Набор критериев поиска по полю
     */
    public static Set<List<FieldSearchCriteria>> toFieldSearchCriterias(Set<List<AttributeFilter>> filters) {

        return isEmpty(filters)
                ? emptySet()
                : filters.stream()
                .map(ConverterUtil::toFieldSearchCriterias)
                .filter(list -> !isEmpty(list))
                .collect(toSet());
    }

    /**
     * Преобразование списка фильтров по атрибуту в список критериев поиска по полю.
     *
     * @param filters фильтры по атрибуту
     * @return Критерии поиска по полю
     */
    public static List<FieldSearchCriteria> toFieldSearchCriterias(List<AttributeFilter> filters) {

        return isEmpty(filters)
                ? emptyList()
                : filters.stream().map(ConverterUtil::toFieldSearchCriteria).collect(toList());
    }

    /**
     * Преобразование фильтра по атрибуту в критерий поиска по полю.
     *
     * @param filter фильтр по атрибуту
     * @return Критерий поиска по полю
     */
    private static FieldSearchCriteria toFieldSearchCriteria(AttributeFilter filter) {

        return toFieldSearchCriteria(
                filter.getAttributeName(), filter.getFieldType(),
                filter.getSearchType(), singletonList(filter.getValue())
        );
    }

    /**
     * Преобразование набора фильтров в набор критериев поиска по полю в соответствии со структурой.
     *
     * @param filters   набор фильтров
     * @param structure структура справочника
     * @return Набор критериев поиска по полю
     */
    public static Set<List<FieldSearchCriteria>> toFieldSearchCriterias(Map<String, String> filters, Structure structure) {

        if (isEmpty(filters))
            return emptySet();

        return singleton(filters.entrySet().stream()
                .map(filter -> toFieldSearchCriteria(filter, structure))
                .filter(Objects::nonNull)
                .collect(toList()));
    }

    /**
     * Преобразование фильтра в критерий поиска по полю в соответствии со структурой.
     *
     * @param filter    фильтр
     * @param structure структура справочника
     * @return Критерий поиска по полю
     */
    private static FieldSearchCriteria toFieldSearchCriteria(Map.Entry<String, String> filter, Structure structure) {

        Structure.Attribute attribute = structure.getAttribute(filter.getKey());
        if (attribute == null) return null;

        Field field = field(attribute);
        return new FieldSearchCriteria(field, SearchTypeEnum.LIKE,
                singletonList(toSearchValue(field, filter.getValue()))
        );
    }

    /**
     * Преобразование ссылок на значения первичных ключей в набор критериев поиска по полям-ссылкам.
     *
     * @param references    ссылки
     * @param primaryValues значения первичных ключей
     * @return Набор критериев поиска по полям-ссылкам
     */
    public static Set<List<FieldSearchCriteria>> toReferenceSearchCriterias(List<Structure.Reference> references,
                                                                            List<String> primaryValues) {
        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        references.forEach(reference -> {

            FieldSearchCriteria criteria = ConverterUtil.toFieldSearchCriteria(reference.getAttribute(),
                    FieldType.REFERENCE, SearchTypeEnum.EXACT, primaryValues);
            fieldSearchCriterias.add(singletonList(criteria));
        });

        return fieldSearchCriterias;
    }

    /**
     * Преобразование ссылок на ненулевые значения первичных ключей в набор критериев поиска по полям-ссылкам.
     *
     * @param references ссылки
     * @return Набор критериев поиска по полям-ссылкам
     */
    public static Set<List<FieldSearchCriteria>> toNotNullSearchCriterias(List<Structure.Reference> references) {

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        references.forEach(reference -> {

            FieldSearchCriteria criteria = ConverterUtil.toFieldSearchCriteria(reference.getAttribute(),
                    FieldType.REFERENCE, SearchTypeEnum.IS_NOT_NULL, NOT_NULL_VALUES);
            fieldSearchCriterias.add(singletonList(criteria));
        });

        return fieldSearchCriterias;
    }

    /**
     * Преобразование поиска значений для поля в критерий поиска по полю.
     *
     * @param fieldName  наименование поля
     * @param fieldType  тип поля
     * @param searchType тип поиска
     * @param values     значения
     * @return Критерий поиска по полю
     */
    public static FieldSearchCriteria toFieldSearchCriteria(String fieldName, FieldType fieldType,
                                                            SearchTypeEnum searchType,
                                                            List<? extends Serializable> values) {
        final Field field = field(fieldName, fieldType);
        return new FieldSearchCriteria(field, searchType, values);
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

    public static String toStringValue(Serializable value) {

        if (value == null) return null;

        if (value instanceof LocalDate) {
            return TimeUtils.format((LocalDate) value);
        }

        if (value instanceof Reference) {
            return ((Reference) value).getValue();
        }

        return String.valueOf(value);
    }

    public static Map<String, Serializable> toStringObjectMap(RefBookRowValue rowValue) {

        Map<String, Serializable> map = new HashMap<>();
        map.put("rowId", rowValue.getId());

        rowValue.getFieldValues().forEach(fieldValue -> map.put(fieldValue.getField(), toPlainValue(fieldValue)));
        return map;
    }

    public static Serializable toPlainValue(FieldValue fieldValue) {

        if (fieldValue == null) return null;

        if (fieldValue.getValue() instanceof Reference) {
            return ((Reference) fieldValue.getValue()).getValue();
        }

        return fieldValue.getValue();
    }

    /**
     * Приведение значения ссылки к значению в соответствии с указанным полем.
     * <p/>
     * См. также {@link ru.i_novus.ms.rdm.api.util.FieldValueUtils#castReferenceValue(String, FieldType)}.
     *
     * @param field поле, на которое указывает ссылка
     * @param value строковое значение ссылки
     * @return Значение поля
     */
    public static Serializable castReferenceValue(Field field, String value) {

        if (field instanceof BooleanField) {
            return Boolean.valueOf(value);
        }

        if (field instanceof DateField) {
            return LocalDate.parse(value, DATE_PATTERN_ERA_FORMATTER);
        }

        if (field instanceof FloatField) {
            return Float.parseFloat(value);
        }

        if (field instanceof IntegerField) {
            return new BigInteger(value);
        }

        if (field instanceof StringField) {
            return value;
        }

        if (field instanceof TreeField) {
            return value;
        }

        throw new RdmException("invalid field type");
    }

    /**
     * Преобразование значения поля в значение для поиска с учётом специальных значений.
     *
     * @param value значение
     * @return Значение для поиска
     */
    public static Serializable toSearchValue(Serializable value) {

        if (value instanceof Reference) {
            return ((Reference) value).getValue();
        }

        return value;
    }

    /**
     * Преобразование строкового значения поля в значение для поиска в соответствии с полем.
     *
     * @param field поле
     * @param value строковое значение
     * @return Значение для поиска
     */
    public static Serializable toSearchValue(Field field, String value) {
        try {
            if (field instanceof BooleanField) {
                return Boolean.valueOf(value);
            }

            if (field instanceof DateField) {
                return LocalDate.parse(value, DATE_PATTERN_ERA_FORMATTER);
            }

            if (field instanceof FloatField) {
                return Float.parseFloat(value);
            }

            if (field instanceof IntegerField) {
                return new BigInteger(value);
            }

            return value;

        } catch (Exception e) {
            throw new UserException("invalid.search.value", e);
        }
    }

    /**
     * Преобразование критерия rdm в критерий vds.
     *
     * @param restCriteria критерий rdm
     * @param count        количество
     * @return Критерий vds
     */
    public static Criteria toCriteria(RestCriteria restCriteria, Integer count) {

        Criteria criteria = new Criteria();
        criteria.setPage(restCriteria.getPageNumber() + BaseDataCriteria.PAGE_SHIFT);
        criteria.setSize(restCriteria.getPageSize());
        criteria.setCount(count);

        return criteria;
    }
}
