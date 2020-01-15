package ru.inovus.ms.rdm.impl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

import javax.persistence.Converter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Converter
public class MapToJsonConverter implements javax.persistence.AttributeConverter<Map<String, Object>, Object> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object convertToDatabaseColumn(Map<String, Object> value) {
        try {
            PGobject out = new PGobject();
            out.setType("json");
            out.setValue(mapper.writeValueAsString(value));
            return out;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize to jsonb field.", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(Object value) {
        try {
            if (value instanceof PGobject && "json".equals(((PGobject) value).getType())) {
                return mapper.convertValue(((PGobject) value).getValue(), HashMap.class);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize from jsonb field.", e);
        }
        return Collections.emptyMap();
    }
}
