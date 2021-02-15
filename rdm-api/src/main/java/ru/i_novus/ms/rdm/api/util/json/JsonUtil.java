package ru.i_novus.ms.rdm.api.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@SuppressWarnings({"java:S1104", "java:S1444", "I-novus:ClassFinalVariablesRule"})
public final class JsonUtil {

    public static ObjectMapper jsonMapper; // all warnings here

    private JsonUtil() {
        throw new UnsupportedOperationException();
    }

    public static String toJsonString(Object o) {
        return toJsonString(jsonMapper, o);
    }

    public static <T> T fromJsonString(String value, Class<T> clazz) {
        return fromJsonString(jsonMapper, value, clazz);
    }

    public static String toJsonString(ObjectMapper jsonMapper, Object o) {
        try {
            return jsonMapper.writeValueAsString(o);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize json value.", e);
        }
    }

    public static <T> T fromJsonString(ObjectMapper jsonMapper, String value, Class<T> clazz) {
        try {
            return jsonMapper.readValue(value, clazz);

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize json value.", e);
        }
    }
}
