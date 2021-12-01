package ru.i_novus.ms.rdm.api.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
        throw new UnsupportedOperationException();
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    public static String toJsonString(Object o) {
        return toJsonString(getMapper(), o);
    }

    public static <T> T fromJsonString(String value, Class<T> clazz) {
        return fromJsonString(getMapper(), value, clazz);
    }

    public static String toJsonString(ObjectMapper mapper, Object o) {
        try {
            return mapper.writeValueAsString(o);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize json value.", e);
        }
    }

    public static <T> T fromJsonString(ObjectMapper mapper, String value, Class<T> clazz) {
        try {
            return mapper.readValue(value, clazz);

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize json value.", e);
        }
    }
}
