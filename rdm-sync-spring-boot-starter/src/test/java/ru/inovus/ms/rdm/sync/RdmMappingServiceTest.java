package ru.inovus.ms.rdm.sync;

import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.service.RdmMappingServiceImpl;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.sync.model.DataTypeEnum.*;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class RdmMappingServiceTest {
    private static final String DATE_FORMAT = "dd.MM.yyyy";

    @InjectMocks
    private RdmMappingServiceImpl rdmMappingService;

    @Test
    public void testMapping(){
        String sysField = "field";
        String rdmField = "field";
        Object result = rdmMappingService.map(new FieldMapping(sysField, INTEGER.getText(), rdmField, INTEGER.getText()), 1);
        assertEquals("При одинаковых типах данных значение не должно измениться", 1, result);

        result = rdmMappingService.map(new FieldMapping(sysField, INTEGER.getText(), rdmField, VARCHAR.getText()), "10");
        assertEquals(BigInteger.TEN, result);

        result = rdmMappingService.map(new FieldMapping(sysField, FLOAT.getText(), rdmField, VARCHAR.getText()), "10.5");
        assertEquals(Float.parseFloat("10.5"), result);

        result = rdmMappingService.map(new FieldMapping(sysField, BOOLEAN.getText(), rdmField, VARCHAR.getText()), "true");
        assertTrue((result instanceof Boolean) && (Boolean) result);

        result = rdmMappingService.map(new FieldMapping(sysField, DATE.getText(), rdmField, VARCHAR.getText()), "15.10.2007");
        assertEquals(LocalDate.of(2007, Month.OCTOBER, 15), result);

        result = rdmMappingService.map(new FieldMapping(sysField, VARCHAR.getText(), rdmField, INTEGER.getText()), 1);
        assertEquals("1", result);

        result = rdmMappingService.map(new FieldMapping(sysField, FLOAT.getText(), rdmField, INTEGER.getText()), 1);
        assertEquals(Float.parseFloat("1"), result);

        try {
            rdmMappingService.map(new FieldMapping(sysField, BOOLEAN.getText(), rdmField, INTEGER.getText()), 1);
            fail("Ожидается ClassCastException");
        }catch (ClassCastException e){
            assertEquals("Ошибка при попытке преобразовать тип bigint в boolean значение: 1", e.getMessage());
        }

        try {
            rdmMappingService.map(new FieldMapping(sysField, DATE.getText(), rdmField, INTEGER.getText()), 1);
            fail("Ожидается ClassCastException");
        }catch (ClassCastException e){
            assertEquals("Ошибка при попытке преобразовать тип bigint в date значение: 1", e.getMessage());
        }

        result = rdmMappingService.map(new FieldMapping(sysField, VARCHAR.getText(), rdmField, BOOLEAN.getText()), true);
        assertEquals("true", result);

        try {
            rdmMappingService.map(new FieldMapping(sysField, DATE.getText(), rdmField, BOOLEAN.getText()), true);
            fail("Ожидается ClassCastException");
        }catch (ClassCastException e){
            assertEquals("Ошибка при попытке преобразовать тип boolean в date значение: true", e.getMessage());
        }

        result = rdmMappingService.map(new FieldMapping(sysField, VARCHAR.getText(), rdmField, DATE.getText()), LocalDate.of(2007, Month.OCTOBER, 15));
        assertEquals("15.10.2007", result);

        result = rdmMappingService.map(new FieldMapping(sysField, VARCHAR.getText(), rdmField, DATE.getText()), Calendar.getInstance().getTime());
        assertEquals(FastDateFormat.getInstance(DATE_FORMAT).format(Calendar.getInstance().getTime()), result);

        try {
            rdmMappingService.map(new FieldMapping(sysField, INTEGER.getText(), rdmField, DATE.getText()),  LocalDate.of(2007, Month.OCTOBER, 15));
            fail("Ожидается ClassCastException");
        }catch (ClassCastException e){
            assertEquals("Ошибка при попытке преобразовать тип date в bigint значение: 2007-10-15", e.getMessage());
        }

        try {
            rdmMappingService.map(new FieldMapping(sysField, JSONB.getText(), rdmField, INTEGER.getText()), 1);
            fail("Ожидается ClassCastException");
        }catch (ClassCastException e){
            assertEquals("Ошибка при попытке преобразовать тип bigint в jsonb значение: 1", e.getMessage());
        }
    }
}
