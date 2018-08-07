package ru.inovus.ms.rdm.util;

import net.n2oapp.criteria.api.Direction;
import net.n2oapp.criteria.api.Sorting;
import org.springframework.data.domain.Sort;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.service.FieldFactoryImpl;
import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.Structure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class ConverterUtil {

    private static FieldFactory fieldFactory = new FieldFactoryImpl();

    private ConverterUtil() {
    }

    public static List<Field> fields(Structure structure) {
        List<Field> fields = new ArrayList<>();
        if (structure != null) {
            Optional.ofNullable(structure.getAttributes()).ifPresent(s ->
                    s.forEach(attribute ->  fields.add(field(attribute)))
            );
        }
        return fields;
    }

    public static Field field(Structure.Attribute attribute) {
        return fieldFactory.createSearchField(attribute.getCode(), attribute.getType());
    }

    public static RowValue rowValue(Row row, Structure structure) {
        List<Field> fields = ConverterUtil.fields(structure);
        return new LongRowValue(fields.stream().map(field -> field.valueOf(row.getData().get(field.getName())))
                .toArray(size -> new FieldValue[size]));
    }

    public static Date date(LocalDateTime date) {
        return date != null ? Date.from(date.atZone(ZoneOffset.UTC).toInstant()) : null;
    }

    public static List<Sorting> sortings(Sort sort) {
        List<Sorting> sortings = new ArrayList<>();
        for (Sort.Order order : sort) {
            sortings.add(new Sorting(order.getProperty(), Direction.valueOf(order.getDirection().name())));
        }
        return sortings;
    }

    public static List<FieldSearchCriteria> getFieldSearchCriteriaList(List<AttributeFilter> attributeFilters) {
        return attributeFilters.stream().map(attrFilter -> new FieldSearchCriteria(
                        fieldFactory.createField(attrFilter.getAttributeName(), attrFilter.getFieldType()),
                        attrFilter.getSearchType(),
                        Collections.singletonList(attrFilter.getValue()))
        ).collect(Collectors.toList());
    }

}
