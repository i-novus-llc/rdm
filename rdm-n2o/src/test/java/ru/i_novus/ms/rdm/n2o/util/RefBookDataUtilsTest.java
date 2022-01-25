package ru.i_novus.ms.rdm.n2o.util;

import org.junit.Test;
import ru.i_novus.ms.rdm.n2o.BaseTest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.DATE_TIME_PATTERN_EUROPEAN_FORMATTER;
import static ru.i_novus.ms.rdm.n2o.util.RefBookDataUtils.*;
import static ru.i_novus.ms.rdm.n2o.utils.StructureTestConstants.*;

public class RefBookDataUtilsTest extends BaseTest {

    private static final LocalDate DATE = LocalDate.now();
    private static final LocalTime TIME = LocalTime.now();
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(DATE, TIME);

    private static final String STRING = "string";
    private static final int INT = 1;
    private static final double FLOAT = 1.1;
    private static final boolean BOOL = true;

    private static final BigInteger BIG_INT = BigInteger.valueOf(INT);
    private static final BigDecimal BIG_DECIM = BigDecimal.valueOf(INT);
    private static final BigDecimal BIG_FLOAT = BigDecimal.valueOf(FLOAT);
    private static final Boolean BOOLEAN = BOOL;

    @Test
    public void testCastFilterValue() {

        assertNull(castFilterValue(ID_ATTRIBUTE, null));

        assertEquals(STRING, castFilterValue(STRING_ATTRIBUTE, STRING));
        assertEquals(BIG_INT, castFilterValue(INTEGER_ATTRIBUTE, INT));
        assertEquals(BIG_FLOAT, castFilterValue(FLOAT_ATTRIBUTE, FLOAT));
        assertEquals(BOOL, castFilterValue(BOOLEAN_ATTRIBUTE, BOOL));
        assertEquals(DATE, castFilterValue(DATE_ATTRIBUTE, DATE));
    }

    @Test
    public void testCastInteger() {

        assertEquals(BIG_INT, castInteger(INT));
        assertEquals(BIG_INT, castInteger(BIG_INT));
        assertEquals(BIG_INT, castInteger(String.valueOf(INT)));
    }

    @Test
    public void testCastFloat() {

        assertEquals(BIG_FLOAT, castFloat(FLOAT));
        assertEquals(BIG_FLOAT, castFloat(BIG_FLOAT));
        assertEquals(BIG_FLOAT, castFloat(String.valueOf(FLOAT)));

        assertEquals(BIG_DECIM, castFloat(INT));
        assertEquals(BIG_DECIM, castFloat(BIG_INT));
        assertEquals(BIG_DECIM, castFloat(String.valueOf(INT)));
    }

    @Test
    public void testCastDate() {

        assertEquals(DATE, castDate(DATE));
        assertEquals(DATE, castDate(DATE_TIME));
        assertEquals(DATE, castDate(DATE_TIME.format(DATE_TIME_PATTERN_EUROPEAN_FORMATTER)));
    }

    @Test
    public void testCastBoolean() {

        assertNull(castBoolean(""));

        assertEquals(BOOL, castBoolean(BOOL));
        assertEquals(BOOL, castBoolean(BOOLEAN));
        assertEquals(BOOL, castBoolean(String.valueOf(BOOL)));

        assertEquals(!BOOL, castBoolean(!BOOL));
        assertEquals(!BOOL, castBoolean(String.valueOf(!BOOL)));
    }

    @Test
    public void testCastBooleanWhenFail() {

        try {
            castBoolean("abracadabra");
            fail(getFailedMessage(IllegalArgumentException.class));

        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("data.filter.bool.is.invalid", getExceptionMessage(e));
        }
    }
}