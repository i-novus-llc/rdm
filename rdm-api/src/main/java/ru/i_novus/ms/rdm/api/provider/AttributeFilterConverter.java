package ru.i_novus.ms.rdm.api.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.TypedParamConverter;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;

import java.io.IOException;

public class AttributeFilterConverter implements TypedParamConverter<AttributeFilter> {

    private final ObjectMapper objectMapper;

    public AttributeFilterConverter(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public Class<AttributeFilter> getType() {
        return AttributeFilter.class;
    }

    @Override
    public AttributeFilter fromString(String value) {
        try {
            return objectMapper.readValue(value, AttributeFilter.class);

        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to convert string '%s' to AttributeFilter", value), e);
        }
    }

    @Override
    public String toString(AttributeFilter value) {
        try {
            return objectMapper.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert from AttributeFilter to string", e);
        }
    }
}
