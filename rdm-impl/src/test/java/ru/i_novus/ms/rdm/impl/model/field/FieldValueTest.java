package ru.i_novus.ms.rdm.impl.model.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.impl.service.diff.DataDiffUtil;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.DateField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.FloatField;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.IntegerField;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FieldValueTest {

    private static final ObjectMapper OBJECT_MAPPER = DataDiffUtil.getMapper();

    @Test
    public void testBigIntegerValue() {

        final BigInteger value = BigInteger.valueOf(4L);
        String json = JsonUtil.toJsonString(OBJECT_MAPPER, value);
        Object restored = JsonUtil.fromJsonString(OBJECT_MAPPER, json, BigInteger.class);
        assertEquals(value, restored);
    }

    @Test
    public void testIntegerFieldValue() {

        final BigInteger value = BigInteger.valueOf(4L);
        FieldValue fieldValue = new IntegerField("text").valueOf(value);
        String jsonFieldValue = JsonUtil.toJsonString(OBJECT_MAPPER, fieldValue);

        FieldValue restored = JsonUtil.fromJsonString(OBJECT_MAPPER, jsonFieldValue, FieldValue.class);
        assertNotNull(restored);
        assertEquals(fieldValue, restored);
    }

    @Test
    public void testBigDecimalValue() {

        final BigDecimal value = BigDecimal.valueOf(4.0);
        String json = JsonUtil.toJsonString(OBJECT_MAPPER, value);
        Object restored = JsonUtil.fromJsonString(OBJECT_MAPPER, json, BigDecimal.class);
        assertEquals(value, restored);
    }

    @Test
    public void testFloatFieldValue() {

        final BigDecimal value = BigDecimal.valueOf(4.0);
        FieldValue fieldValue = new FloatField("text").valueOf(value);
        String jsonFieldValue = JsonUtil.toJsonString(OBJECT_MAPPER, fieldValue);

        FieldValue restored = JsonUtil.fromJsonString(OBJECT_MAPPER, jsonFieldValue, FieldValue.class);
        assertNotNull(restored);
        assertEquals(fieldValue, restored);
    }

    @Test
    public void testDateValue() {

        final LocalDate value = LocalDate.of(2021, 2, 3);
        String json = JsonUtil.toJsonString(OBJECT_MAPPER, value);
        Object restored = JsonUtil.fromJsonString(OBJECT_MAPPER, json, LocalDate.class);
        assertEquals(value, restored);
    }

    @Test
    public void testDateFieldValue() {

        final LocalDate value = LocalDate.of(2021, 2, 3);
        FieldValue fieldValue = new DateField("text").valueOf(value);
        String jsonFieldValue = JsonUtil.toJsonString(OBJECT_MAPPER, fieldValue);

        FieldValue restored = JsonUtil.fromJsonString(OBJECT_MAPPER, jsonFieldValue, FieldValue.class);
        assertNotNull(restored);
        assertEquals(fieldValue, restored);
    }
}
