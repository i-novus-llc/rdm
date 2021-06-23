package ru.inovus.ms.rdm.api.provider;

import net.n2oapp.platform.jaxrs.TypedParamConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class IsoLocalDateParamConverter implements TypedParamConverter<LocalDate> {

    @Override
    public Class<LocalDate> getType() {
        return LocalDate.class;
    }

    @Override
    public LocalDate fromString(String value) {
        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public String toString(LocalDate value) {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

}
