package ru.i_novus.ms.rdm.api.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class JsonUtil {

    public static ObjectMapper jsonMapper; // NOSONAR

    private JsonUtil() {
        throw new UnsupportedOperationException();
    }

    public static String toJsonString(Object o) {
        try {
            return jsonMapper.writeValueAsString(o);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize json value.", e);
        }
    }

    public static <T> T fromJsonString(String value, Class<T> clazz) {
        try {
            return jsonMapper.readValue(value, clazz);

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize json value.", e);
        }
    }
}
