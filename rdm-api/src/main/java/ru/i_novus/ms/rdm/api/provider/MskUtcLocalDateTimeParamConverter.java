package ru.i_novus.ms.rdm.api.provider;

import net.n2oapp.platform.jaxrs.TypedParamConverter;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

import java.time.LocalDateTime;

public class MskUtcLocalDateTimeParamConverter implements TypedParamConverter<LocalDateTime> {

    private TypedParamConverter<LocalDateTime> original;

    public MskUtcLocalDateTimeParamConverter(TypedParamConverter<LocalDateTime> original) {
        this.original = original;
    }

    @Override
    public Class<LocalDateTime> getType() {
        return original.getType();
    }

    @Override
    public LocalDateTime fromString(String s) {
        return TimeUtils.zonedToUtc(original.fromString(s));
    }

    @Override
    public String toString(LocalDateTime localDateTime) {
        return  original.toString(TimeUtils.utcToZoned(localDateTime));
    }
}
