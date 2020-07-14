package ru.i_novus.ms.rdm.api.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.n2oapp.platform.jaxrs.TypedParamConverter;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;

import java.io.IOException;

import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.jsonMapper;

public class AttributeFilterConverter implements TypedParamConverter<AttributeFilter> {

    @Override
    public Class<AttributeFilter> getType() {
        return AttributeFilter.class;
    }

    @Override
    public AttributeFilter fromString(String value) {
        try {
            return jsonMapper.readValue(value, AttributeFilter.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to convert string '%s' to AttributeFilter", value), e);
        }
    }

    @Override
    public String toString(AttributeFilter value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert from AttributeFilter to string", e);
        }
    }
}
