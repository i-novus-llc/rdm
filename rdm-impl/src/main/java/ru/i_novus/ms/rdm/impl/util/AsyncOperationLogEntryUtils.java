package ru.i_novus.ms.rdm.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.jsonMapper;

public final class AsyncOperationLogEntryUtils {

    private AsyncOperationLogEntryUtils() {
        throw new UnsupportedOperationException();
    }

    public static void setResult(Object result, AsyncOperationLogEntryEntity entity) {

        if (result == null)
            return;

        try {
            entity.setResult(jsonMapper.writeValueAsString(result));

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize jsonb value.", e);
        }
    }
}
