package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.DateRangeValidationValue;

import java.time.LocalDate;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.validation.resolver.DateRangeAttributeValidationResolver.DATE_RANGE_EXCEPTION_CODE;

public class DateRangeAttributeValidationResolverTest {


    @Test
    public void testResolve() {

        final String TEST_ATTRIBUTE = "test_attribute";
        final LocalDate min = LocalDate.of(2018, 12, 2);
        final LocalDate max = LocalDate.of(2019, 11, 1);
        final String wrongEntityValue = "02.12.2018;;";
        final String entityValue = "02.12.2018;01.11.2019";
        final LocalDate before = LocalDate.of(2018, 12, 1);
        final LocalDate in = LocalDate.of(2019, 1, 1);
        final LocalDate after = LocalDate.of(2020, 11, 1);
        final Structure.Attribute wrongAttribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
        final Structure.Attribute attribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.DATE, null);

        //создание с неправильным типом типом атрибута
        try {
            new DateRangeAttributeValidationResolver(wrongAttribute, min, max);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }

        //создание validationValue с из неправильной строки
        try {
            new DateRangeValidationValue().valueFromString(wrongEntityValue);
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }

        //проверка работы
        DateRangeValidationValue validationValue = new DateRangeValidationValue().valueFromString(entityValue);
        assertEquals(min, validationValue.getMin());
        assertEquals(max, validationValue.getMax());

        DateRangeAttributeValidationResolver resolver = new DateRangeAttributeValidationResolver(attribute, validationValue);
        Message actual = resolver.resolve(before);
        assertEquals(DATE_RANGE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(after);
        assertEquals(DATE_RANGE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(in));
    }
}