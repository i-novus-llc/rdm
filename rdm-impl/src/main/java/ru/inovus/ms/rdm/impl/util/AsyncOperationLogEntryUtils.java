package ru.inovus.ms.rdm.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class AsyncOperationLogEntryUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AsyncOperationLogEntryUtils() {throw new UnsupportedOperationException();}

    public static AsyncOperationLogEntryEntity setResult(Object result, AsyncOperationLogEntryEntity entity) {
        try {
            entity.setResult(MAPPER.writeValueAsString(result));
            return entity;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize jsonb value.", e);
        }
    }

    public static <T> T getResult(Class<? extends T> forClass, AsyncOperationLogEntryEntity entity) {
        if (entity.getResult() == null || forClass == Void.TYPE)
            return null;
        try {
            return MAPPER.readValue(entity.getResult(), forClass);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize jsonb value.", e);
        }
    }

    public static Map<String, Object> getPayload(AsyncOperationLogEntryEntity entity) {
        if (entity.getPayload() == null)
            return null;
        try {
            return MAPPER.readValue(entity.getPayload(), HashMap.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize jsonb value.", e);
        }
    }

    public static String getPayloadAsJson(Map<String, Object> payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize jsonb value.", e);
        }
    }

}
