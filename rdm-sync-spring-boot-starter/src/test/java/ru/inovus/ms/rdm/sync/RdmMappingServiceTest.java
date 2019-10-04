package ru.inovus.ms.rdm.sync;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.sync.service.RdmMappingServiceImpl;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Month;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.sync.model.DataTypeEnum.*;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class RdmMappingServiceTest {

    @InjectMocks
    private RdmMappingServiceImpl rdmMappingService;

    @Test
    public void testInteger() {
        Object result = rdmMappingService.map(FieldType.INTEGER, INTEGER, BigInteger.ONE);
        assertEquals(BigInteger.ONE, result);

        result = rdmMappingService.map(FieldType.INTEGER, VARCHAR, 1);
        assertEquals("1", result);

        result = rdmMappingService.map(FieldType.INTEGER, FLOAT, 1);
        assertEquals(Float.parseFloat("1"), result);

        try {
            rdmMappingService.map(FieldType.INTEGER, BOOLEAN, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Ошибка при попытке преобразовать тип INTEGER в BOOLEAN значение: 1", e.getMessage());
        }

        try {
            rdmMappingService.map(FieldType.INTEGER, DATE, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Ошибка при попытке преобразовать тип INTEGER в DATE значение: 1", e.getMessage());
        }

        try {
            rdmMappingService.map(FieldType.INTEGER, JSONB, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Ошибка при попытке преобразовать тип INTEGER в JSONB значение: 1", e.getMessage());
        }
    }

    @Test
    public void testString() {
        Object result = rdmMappingService.map(FieldType.STRING, VARCHAR, "10");
        assertEquals("10", result);

        result = rdmMappingService.map(FieldType.STRING, INTEGER, "10");
        assertEquals(BigInteger.TEN, result);

        result = rdmMappingService.map(FieldType.STRING, FLOAT, "10.5");
        assertEquals(Float.parseFloat("10.5"), result);

        result = rdmMappingService.map(FieldType.STRING, BOOLEAN, "true");
        assertTrue((result instanceof Boolean) && (Boolean) result);

        result = rdmMappingService.map(FieldType.STRING, DATE, "2007-10-15");
        assertEquals(LocalDate.of(2007, Month.OCTOBER, 15), result);

        try {
            rdmMappingService.map(FieldType.STRING, JSONB, "1");
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Ошибка при попытке преобразовать тип STRING в JSONB значение: 1", e.getMessage());
        }
    }

    @Test
    public void testBoolean() {
        Object result = rdmMappingService.map(FieldType.BOOLEAN, BOOLEAN, true);
        assertEquals(true, result);

        result = rdmMappingService.map(FieldType.BOOLEAN, VARCHAR, true);
        assertEquals("true", result);

        try {
            rdmMappingService.map(FieldType.BOOLEAN, DATE, true);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Ошибка при попытке преобразовать тип BOOLEAN в DATE значение: true", e.getMessage());
        }
//      В рдм поле типа boolean. Значение не присутстствует. Необходимо, чтобы они смаппились на
//      дефолтный false.
        result = rdmMappingService.map(FieldType.BOOLEAN, VARCHAR, null);
        assertEquals("false", result);
        result = rdmMappingService.map(FieldType.BOOLEAN, BOOLEAN, null);
        assertEquals(false, result);
    }

    @Test
    public void testDate() {
        LocalDate date = LocalDate.of(2007, Month.OCTOBER, 15);

        Object result = rdmMappingService.map(FieldType.DATE, DATE, date);
        assertEquals(date, result);

        result = rdmMappingService.map(FieldType.DATE, VARCHAR, date);
        assertEquals("2007-10-15", result);

        try {
            rdmMappingService.map(FieldType.DATE, INTEGER, date);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Ошибка при попытке преобразовать тип DATE в INTEGER значение: 2007-10-15", e.getMessage());
        }
    }

    @Test
    public void testReference() {
        Reference reference = new Reference("1", "Moscow");
        Object result = rdmMappingService.map(FieldType.REFERENCE, JSONB, new Reference("1", "Moscow"));
        assertEquals(reference, result);

        try {
            rdmMappingService.map(FieldType.INTEGER, JSONB, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Ошибка при попытке преобразовать тип INTEGER в JSONB значение: 1", e.getMessage());
        }
    }
}
