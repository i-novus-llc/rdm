package ru.inovus.ms.rdm.provider;

import ru.inovus.ms.rdm.util.TimeUtils;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

@Provider
public class RdmN2oParamConverterProvider implements ParamConverterProvider {

    private RdmParamConverterProvider rdmParamConverterProvider = new RdmParamConverterProvider();

    private LocalDateTimeParamConverter localDateTimeParamConverter = new LocalDateTimeParamConverter();

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (LocalDateTime.class.equals(rawType))
            //noinspection unchecked
            return (ParamConverter<T>) localDateTimeParamConverter;
        else
            return rdmParamConverterProvider.getConverter(rawType, genericType, annotations);
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
}