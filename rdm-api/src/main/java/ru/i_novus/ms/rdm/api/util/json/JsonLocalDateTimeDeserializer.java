package ru.i_novus.ms.rdm.api.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

import java.io.IOException;
import java.time.LocalDateTime;

public class JsonLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    public JsonLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return TimeUtils.zonedToUtc(TimeUtils.parseLocalDateTime(p.getValueAsString()));
    }
}
