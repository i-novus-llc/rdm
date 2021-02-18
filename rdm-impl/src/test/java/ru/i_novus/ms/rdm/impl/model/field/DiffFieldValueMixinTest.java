package ru.i_novus.ms.rdm.impl.model.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.icu.math.BigDecimal;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.impl.service.diff.DataDiffUtil;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.*;

import java.math.BigInteger;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DiffFieldValueMixinTest {

    private static final ObjectMapper vdsObjectMapper = DataDiffUtil.getVdsObjectMapper();

    @Test
    public void testJson() {

        testJson(IntegerField.class, BigInteger.ONE, BigInteger.TWO);
        testJson(FloatField.class, new BigDecimal("1"), new BigDecimal("2"));
        testJson(StringField.class, "one", "two");
        testJson(DateField.class,
                LocalDate.of(2021, 2, 3),
                LocalDate.of(2020, 12, 31)
        );
        testJson(BooleanField.class, Boolean.FALSE, Boolean.TRUE);
        testJson(ReferenceField.class, "1-one-один", "2-two-два"); // displayValue
        testJson(IntegerStringField.class, "1", "2");
    }

    private <T extends Field, V> void testJson(Class<T> clazz, V oldValue, V newValue) {

        T field = createField(clazz);
        if (field == null)
            return;

        testValueJson(field, null, null);
        testValueJson(field, null, newValue);
        testValueJson(field, oldValue, null);
        testValueJson(field, oldValue, newValue);
        testValueJson(field, newValue, newValue);
    }

    private <T> T createField(Class<T> clazz) {
        try {
            return (T) clazz.getConstructor(String.class).newInstance("text");

        } catch (ReflectiveOperationException e) {
            fail();
            return null;
        }
    }

    private <T extends Field, V> void testValueJson(T field, V oldValue, V newValue) {

        DiffFieldValue value = new DiffFieldValue(field, oldValue, newValue, toStatus(oldValue, newValue));

        String json = JsonUtil.toJsonString(vdsObjectMapper, value);
        DiffFieldValue restored = JsonUtil.fromJsonString(vdsObjectMapper, json, DiffFieldValue.class);
        assertEquals("Error when class deserialization from json\n:" + json + "\n:", value.getClass(), restored.getClass());
        assertEquals("Error when value deserialization from json\n:" + json + "\n:", value, restored);
    }

    private <V> DiffStatusEnum toStatus(V oldValue, V newValue) {

        if (oldValue == null && newValue == null)
            return null;

        if (oldValue == null)
            return DiffStatusEnum.INSERTED;

        if (newValue == null)
            return DiffStatusEnum.DELETED;

        if (!oldValue.equals(newValue))
            return DiffStatusEnum.UPDATED;

        return null;
    }
}
