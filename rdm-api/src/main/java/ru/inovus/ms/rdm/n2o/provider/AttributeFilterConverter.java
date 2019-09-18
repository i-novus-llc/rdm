package ru.inovus.ms.rdm.n2o.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.n2oapp.platform.jaxrs.TypedParamConverter;
import ru.inovus.ms.rdm.n2o.model.version.AttributeFilter;

import java.io.IOException;

public class AttributeFilterConverter implements TypedParamConverter<AttributeFilter> {

    @Override
    public Class<AttributeFilter> getType() {
        return AttributeFilter.class;
    }

    @Override
    public AttributeFilter fromString(String value) {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule jtm = new JavaTimeModule();
        mapper.registerModule(jtm);
        try {
            return mapper.readValue(value, AttributeFilter.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to convert string '%s' to AttributeFilter", value), e);
        }
    }

    @Override
    public String toString(AttributeFilter value) {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule jtm = new JavaTimeModule();
        mapper.registerModule(jtm);
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert from AttributeFilter to string", e);
        }
    }
}
