package ru.inovus.ms.rdm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.Passport;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;

@Provider
public class RdmParamConverterProvider implements ParamConverterProvider {

    private LocalDateTimeParamConverter localDateParamConverter = new LocalDateTimeParamConverter();

    private OffsetDateTimeParamConverter offsetDateTimeParamConverter = new OffsetDateTimeParamConverter();

    private PassportConverter passportConverter = new PassportConverter();

    private AttributeFilterConverter attributeFilterConverter = new AttributeFilterConverter();

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (LocalDateTime.class.equals(rawType))
            //noinspection unchecked
            return (ParamConverter<T>) localDateParamConverter;
        else if (rawType.isEnum())
            //noinspection unchecked
            return (ParamConverter<T>) new EnumParamConverter(rawType);
        else if (OffsetDateTime.class.equals(rawType))
            //noinspection unchecked
            return (ParamConverter<T>) offsetDateTimeParamConverter;
        else if (Passport.class.equals(rawType))
            //noinspection unchecked
            return (ParamConverter<T>) passportConverter;
        else if (AttributeFilter.class.equals(rawType))
            //noinspection unchecked
            return (ParamConverter<T>) attributeFilterConverter;
        return null;
    }

    private static class LocalDateTimeParamConverter implements ParamConverter<LocalDateTime> {

        @Override
        public LocalDateTime fromString(String str) {
            return TimeUtils.parseLocalDateTime(str);
        }

        @Override
        public String toString(LocalDateTime value) {
            return TimeUtils.format(value);
        }
    }


    private static class OffsetDateTimeParamConverter implements ParamConverter<OffsetDateTime> {

        @Override
        public OffsetDateTime fromString(String str) {
            return TimeUtils.parseOffsetDateTime(str);
        }

        @Override
        public String toString(OffsetDateTime value) {
            return TimeUtils.format(value);
        }
    }

    private static class EnumParamConverter<T extends Enum> implements ParamConverter<T> {

        private Class<T> type;

        EnumParamConverter(Class<T> type) {
            this.type = type;
        }

        @Override
        public T fromString(String value) {
            if (value == null) return null;
            return Arrays.stream(type.getEnumConstants())
                    .filter(c -> value.equals(c.name()))
                    .findAny().orElse(null);
        }

        @Override
        public String toString(T value) {
            if (value == null) return null;
            return value.name();
        }
    }

    private static class PassportConverter implements ParamConverter<Passport> {
        @Override
        public Passport fromString(String value) {
            try {
                return new ObjectMapper().readValue(value, Passport.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to convert from json to Passport", e);
            }
        }

        @Override
        public String toString(Passport value) {
            try {
                return new ObjectMapper().writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to convert from Passport to json", e);
            }
        }
    }

    private static class AttributeFilterConverter implements ParamConverter<AttributeFilter> {

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
}