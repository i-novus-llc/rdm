package ru.inovus.ms.rdm.provider;

import net.n2oapp.platform.jaxrs.TypedParamConverter;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.time.OffsetDateTime;

public class OffsetDateTimeParamConverter implements TypedParamConverter<OffsetDateTime> {

    @Override
    public OffsetDateTime fromString(String str) {
        return TimeUtils.parseOffsetDateTime(str);
    }

    @Override
    public String toString(OffsetDateTime value) {
        return TimeUtils.format(value);
    }

    @Override
    public Class<OffsetDateTime> getType() {
        return OffsetDateTime.class;
    }
}
