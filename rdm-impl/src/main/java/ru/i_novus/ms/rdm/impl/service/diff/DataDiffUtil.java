package ru.i_novus.ms.rdm.impl.service.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.i_novus.ms.rdm.impl.provider.VdsMapperConfigurer;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static ru.i_novus.ms.rdm.api.util.StringUtils.addSingleQuotes;
import static ru.i_novus.ms.rdm.api.util.StringUtils.toDoubleQuotes;
import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.fromJsonString;
import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.toJsonString;

public class DataDiffUtil {

    private static final ObjectMapper vdsObjectMapper = createVdsObjectMapper();

    private static final String PRIMARY_FORMAT = "%s=%s";

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private DataDiffUtil() {
        // Nothing to do.
    }

    public static ObjectMapper getVdsObjectMapper() {
        return vdsObjectMapper;
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
        return toJsonString(vdsObjectMapper, diffRowValue);
    }

    public static DiffRowValue fromDataDiffValues(String dataDiffValues) {
        return (dataDiffValues != null) ? fromJsonString(vdsObjectMapper, dataDiffValues, DiffRowValue.class) : null;
    }

    public static ObjectMapper createVdsObjectMapper() {

        ObjectMapper jsonMapper = new ObjectMapper();
        new VdsMapperConfigurer().configure(jsonMapper);

        return jsonMapper;
    }
}
