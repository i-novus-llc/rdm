package ru.inovus.ms.rdm.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import static ru.inovus.ms.rdm.api.util.json.JsonUtil.jsonMapper;

public final class AsyncOperationLogEntryUtils {

    private AsyncOperationLogEntryUtils() {throw new UnsupportedOperationException();}

    public static void setResult(Object result, AsyncOperationLogEntryEntity entity) {
        if (result != null) {
            try {
                entity.setResult(jsonMapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot serialize jsonb value.", e);
            }
        }
    }

}
