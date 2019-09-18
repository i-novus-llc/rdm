package ru.inovus.ms.rdm.n2o.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import ru.inovus.ms.rdm.n2o.util.TimeUtils;

import java.io.IOException;
import java.time.LocalDate;

public class JsonLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return TimeUtils.parseLocalDate(p.getValueAsString());
    }
}
