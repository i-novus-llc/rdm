package ru.inovus.ms.rdm.api.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

    public static ObjectMapper jsonMapper; // NOSONAR

    private JsonUtil() {throw new UnsupportedOperationException();}

    public static String getAsJson(Object obj) {
        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize json value.", e);
        }
    }

}
