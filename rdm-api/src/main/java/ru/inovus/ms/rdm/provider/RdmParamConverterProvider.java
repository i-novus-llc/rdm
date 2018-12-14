package ru.inovus.ms.rdm.provider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.util.TimeUtils;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Provider
public class RdmParamConverterProvider implements ParamConverterProvider {

    private LocalDateTimeParamConverter localDateParamConverter = new LocalDateTimeParamConverter();

    private OffsetDateTimeParamConverter offsetDateTimeParamConverter = new OffsetDateTimeParamConverter();

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
        else if (AttributeFilter.class.equals(rawType))
            //noinspection unchecked
            return (ParamConverter<T>) attributeFilterConverter;
        else if (Map.class.isAssignableFrom(rawType)){
            MapConverter mapConverter;
            if (genericType instanceof ParameterizedType &&
                    ((ParameterizedType) genericType).getActualTypeArguments().length >= 2 &&
                    ((ParameterizedType) genericType).getActualTypeArguments()[1] instanceof Class)
                mapConverter = new MapConverter((Class) ((ParameterizedType) genericType).getActualTypeArguments()[1]);
            else mapConverter = new MapConverter(Object.class);
            //noinspection unchecked
            return (ParamConverter<T>) mapConverter;
        } else if (List.class.isAssignableFrom(rawType)) {
            ListConverter listConverter;
            if (genericType instanceof ParameterizedType &&
                    ((ParameterizedType) genericType).getActualTypeArguments().length > 0 &&
                    ((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof Class)
                listConverter = new ListConverter((Class) ((ParameterizedType) genericType).getActualTypeArguments()[0]);
            else listConverter = new ListConverter(Object.class);
            //noinspection unchecked
            return (ParamConverter<T>) listConverter;
        }
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


    private static class MapConverter implements ParamConverter<Map> {

        JavaType type;
        ObjectMapper mapper;

        public MapConverter(Class valueClass) {
            this.mapper = new ObjectMapper();
            this.type = mapper.getTypeFactory().constructMapType(HashMap.class, String.class, valueClass);
        }

        @Override
        public Map fromString(String value) {
            try {
                return mapper.readValue(value, type);
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Failed to convert string '%s' to Map", value), e);
            }
        }

        @Override
        public String toString(Map value) {
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to convert from Map to string", e);
            }
        }
    }

    private static class ListConverter implements ParamConverter<List> {

        JavaType type;
        ObjectMapper mapper;

        public ListConverter(Class valueClass) {
            this.mapper = new ObjectMapper();
            JavaTimeModule jtm = new JavaTimeModule();
            mapper.registerModule(jtm);
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());
            mapper.registerModule(simpleModule);
            this.type = mapper.getTypeFactory().constructCollectionType(ArrayList.class, valueClass);
        }

        @Override
        public List fromString(String value) {
            try {
                return mapper.readValue(value, type);
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Failed to convert string '%s' to List", value), e);
            }
        }

        @Override
        public String toString(List value) {
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to convert from List to string", e);
            }
        }
    }

    public static class BigDecimalPlainSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.stripTrailingZeros().toPlainString());
        }
        @Override
        public void serializeWithType(BigDecimal value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
            typeSer.writeTypePrefixForScalar(value, gen);
            serialize(value, gen, null);
            typeSer.writeTypeSuffixForScalar(value, gen);            }
    }

}