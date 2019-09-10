package ru.inovus.ms.rdm.util.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.io.IOException;
import java.time.LocalDateTime;

public class RdmN2oJsonLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

    public RdmN2oJsonLocalDateTimeSerializer() {
        super(LocalDateTime.class);
    }

    public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(TimeUtils.format(value));
    }
}