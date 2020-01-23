package ru.inovus.ms.rdm.api.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.registerModule(new JavaTimeModule());
    }

    private JsonUtil() {throw new UnsupportedOperationException();}

    public static String getAsJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize json value.", e);
        }
    }

}
