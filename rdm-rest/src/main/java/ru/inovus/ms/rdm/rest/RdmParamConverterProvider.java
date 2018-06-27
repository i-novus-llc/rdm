package ru.inovus.ms.rdm.rest;

import ru.inovus.ms.rdm.util.TimeUtils;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Arrays;

@Provider
public class RdmParamConverterProvider implements javax.ws.rs.ext.ParamConverterProvider {

    private LocalDateTimeParamConverter localDateParamConverter = new LocalDateTimeParamConverter();

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (LocalDateTime.class.equals(rawType))
            //noinspection unchecked
            return (ParamConverter<T>) localDateParamConverter;
        else if (rawType.isEnum())
            //noinspection unchecked
            return (ParamConverter<T>) new EnumParamConverter(rawType);
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
}