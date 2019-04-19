package ru.inovus.ms.rdm.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;

public class JsonLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(TimeUtils.format(TimeUtils.utcToZoned(value)));
    }
}