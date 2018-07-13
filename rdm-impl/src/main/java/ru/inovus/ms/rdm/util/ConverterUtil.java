package ru.inovus.ms.rdm.util;

import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ConverterUtil {
    private ConverterUtil() {
    }

    public static List<Field> structureToFields(Structure structure, FieldFactory fieldFactory) {
        List<Field> fields = new ArrayList<>();
        if (structure != null) {
            Optional.ofNullable(structure.getAttributes()).ifPresent(s ->
                    s.forEach(attribute ->  fields.add(attributeToField(attribute, fieldFactory)))
            );
        }
        return fields;
    }

    public static Field attributeToField(Structure.Attribute attribute, FieldFactory fieldFactory) {
        return fieldFactory.createField(attribute.getCode(), attribute.getType());
    }

    public static RowValue rowValue(Row row, Structure structure, FieldFactory fieldFactory) {
        List<Field> fields = ConverterUtil.structureToFields(structure, fieldFactory);
        return new LongRowValue(fields.stream().map(field -> field.valueOf(row.getData().get(field.getName())))
                .toArray(size -> new FieldValue[size]));
    }

    public static Date localDateTimeToDate(LocalDateTime date) {
        return date != null ? Date.from(date.atZone(ZoneOffset.UTC).toInstant()) : null;
    }
}
