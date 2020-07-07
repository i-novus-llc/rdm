package ru.inovus.ms.rdm.api.util.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import java.io.IOException;
import java.time.LocalDateTime;

public class JsonLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

    public JsonLocalDateTimeSerializer() {
        super(LocalDateTime.class);
    }

    public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(TimeUtils.format(TimeUtils.utcToZoned(value)));
    }
}