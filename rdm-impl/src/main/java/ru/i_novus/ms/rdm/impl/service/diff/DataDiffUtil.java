package ru.i_novus.ms.rdm.impl.service.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.RestObjectMapperConfigurer;
import ru.i_novus.ms.rdm.impl.provider.VdsMapperConfigurer;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.api.util.StringUtils.addSingleQuotes;
import static ru.i_novus.ms.rdm.api.util.StringUtils.toDoubleQuotes;
import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.fromJsonString;
import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.toJsonString;

public class DataDiffUtil {

    private static final ObjectMapper OBJECT_MAPPER = createVdsObjectMapper();

    public static ObjectMapper createVdsObjectMapper() {

        ObjectMapper objectMapper = new ObjectMapper();
        RestObjectMapperConfigurer.configure(objectMapper, singletonList(new VdsMapperConfigurer()));

        return objectMapper;
    }

    private static final String PRIMARY_FORMAT = "%s=%s";

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private DataDiffUtil() {
        // Nothing to do.
    }

    public static ObjectMapper getMapper() {
        return OBJECT_MAPPER;
    }

    public static String toPrimaryString(String name, Object value) {
        return String.format(PRIMARY_FORMAT, name, toPrimaryValue(value));
    }

    public static String toPrimaryValue(Object value) {

        if (value instanceof LocalDate)
            return addSingleQuotes(DATE_FORMATTER.format((LocalDate) value));

        if (value instanceof String)
            return toDoubleQuotes((String) value);

        return String.valueOf(value);
    }

    public static String toDataDiffValues(DiffRowValue diffRowValue) {
        return toJsonString(OBJECT_MAPPER, diffRowValue);
    }

    public static DiffRowValue fromDataDiffValues(String dataDiffValues) {
        return (dataDiffValues != null) ? fromJsonString(OBJECT_MAPPER, dataDiffValues, DiffRowValue.class) : null;
    }
}
