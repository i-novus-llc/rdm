package ru.inovus.ms.rdm.n2o.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.inovus.ms.rdm.n2o.util.TimeUtils;

import java.io.IOException;
import java.time.LocalDateTime;

public class RdmN2oJsonLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    public RdmN2oJsonLocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return TimeUtils.parseLocalDateTime(p.getValueAsString());
    }
}
