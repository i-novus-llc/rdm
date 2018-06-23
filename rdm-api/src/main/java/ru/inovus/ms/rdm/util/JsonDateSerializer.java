package ru.inovus.ms.rdm.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;

public class JsonDateSerializer extends JsonSerializer<LocalDateTime> {

    public JsonDateSerializer() {
    }

    public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(TimeUtils.format(value));
    }
}