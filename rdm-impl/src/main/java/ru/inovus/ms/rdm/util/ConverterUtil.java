package ru.inovus.ms.rdm.util;

import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.model.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConverterUtil {
    private ConverterUtil() {
    }

    public static List<Field> structureToFields(Structure structure, FieldFactory fieldFactory) {
        List<Field> fields = new ArrayList<>();
        if (structure != null) {
            Optional.ofNullable(structure.getAttributes()).ifPresent(s ->
                    s.forEach(metaField -> {
                        Field field = fieldFactory.createField(metaField.getAttributeName(), metaField.getType());
                        fields.add(field);
                    })
            );
        }
        return fields;
    }

}
