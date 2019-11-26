package ru.inovus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.DateRangeAttributeValidation;
import ru.inovus.ms.rdm.impl.validation.resolver.DateRangeAttributeValidationResolver;

import java.time.LocalDate;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.impl.validation.resolver.DateRangeAttributeValidationResolver.DATE_RANGE_EXCEPTION_CODE;

public class DateRangeAttributeValidationResolverTest {

    private final String TEST_ATTRIBUTE = "test_attribute";
    private final String wrongEntityValue = "02.12.2018;;";
    private final String entityValue = "02.12.2018;01.11.2019";
    private final LocalDate min = LocalDate.of(2018, 12, 2);
    private final LocalDate max = LocalDate.of(2019, 11, 1);
    private final LocalDate before = LocalDate.of(2018, 12, 1);
    private final LocalDate in = LocalDate.of(2019, 1, 1);
    private final LocalDate after = LocalDate.of(2020, 11, 1);
    private final Structure.Attribute wrongAttribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
    private final Structure.Attribute attribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.DATE, null);

    /**
     * создание DateRangeAttributeValidationResolver с неправильным типом атрибута
     */
    @Test
    public void testInvalidType() {
        try {
            new DateRangeAttributeValidationResolver(wrongAttribute, min, max);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }
    }

    /**
     * заполнение DateRangeAttributeValidation из строки
     */
    @Test
    public void testInvalidStringValue() {
        //из неправильной строки
        try {
            new DateRangeAttributeValidation().valueFromString(wrongEntityValue);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }
        //из правильной строки
        DateRangeAttributeValidation validation = new DateRangeAttributeValidation().valueFromString(entityValue);
        assertEquals(min, validation.getMin());
        assertEquals(max, validation.getMax());
    }

    @Test
    public void testResolve() {
        DateRangeAttributeValidationResolver resolver = new DateRangeAttributeValidationResolver(attribute, min, max);
        Message actual = resolver.resolve(before);
        assertEquals(DATE_RANGE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(after);
        assertEquals(DATE_RANGE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(in));
    }
}