package ru.i_novus.ms.rdm.impl.service.diff;

import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.i_novus.ms.rdm.impl.service.diff.DataDiffUtil.*;

public class DataDiffUtilTest {

    private static final String VERSION_ATTRIBUTE_INT = "int";
    private static final String VERSION_ATTRIBUTE_FLOAT = "float";
    private static final String VERSION_ATTRIBUTE_STR = "str";
    private static final String VERSION_ATTRIBUTE_DATE = "date";
    private static final String VERSION_ATTRIBUTE_BOOL = "bool";
    private static final String VERSION_ATTRIBUTE_REF = "ref";

    @Test
    public void testToPrimaryString() {

        assertEquals("name=\"text\"", toPrimaryString("name", "text"));
    }

    @Test
    public void testToPrimaryValue() {

        assertEquals("10", toPrimaryValue(BigInteger.valueOf(10L)));
        assertEquals("\"text\"", toPrimaryValue("text"));
        assertEquals("'2021-02-03'", toPrimaryValue(LocalDate.of(2021, 2, 3)));
    }

    @Test
    @SuppressWarnings({"rawtypes","unchecked"})
    public void testDataDiffValues() {

        Field intField = new IntegerField(VERSION_ATTRIBUTE_INT);
        Field floatField = new FloatField(VERSION_ATTRIBUTE_FLOAT);
        Field strField = new StringField(VERSION_ATTRIBUTE_STR);
        Field dateField = new DateField(VERSION_ATTRIBUTE_DATE);
        Field boolField = new StringField(VERSION_ATTRIBUTE_BOOL);
        Field refField = new ReferenceField(VERSION_ATTRIBUTE_REF);

        DiffRowValue value = new DiffRowValue(asList(
                new DiffFieldValue(intField, null, BigInteger.valueOf(1L), null),
                new DiffFieldValue(floatField, BigDecimal.valueOf(3.3), null, null),
                new DiffFieldValue(strField, null, "two", null),
                new DiffFieldValue(dateField, null, LocalDate.of(2021, 2, 3), DiffStatusEnum.INSERTED),
                new DiffFieldValue(boolField, Boolean.FALSE, Boolean.TRUE, DiffStatusEnum.UPDATED),
                new DiffFieldValue(refField, "ref1", null, DiffStatusEnum.DELETED)
        ), DiffStatusEnum.UPDATED);

        String json = toDataDiffValues(value);
        DiffRowValue restored = fromDataDiffValues(json);
        assertEquals("Error when class deserialization from json\n:" + json + "\n:", value.getClass(), restored.getClass());
        assertEquals("Error when value deserialization from json\n:" + json + "\n:", value, restored);
    }
}