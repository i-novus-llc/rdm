package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.DateRangeValidationValue;
import ru.inovus.ms.rdm.model.validation.FloatRangeValidationValue;

import java.math.BigDecimal;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.validation.resolver.FloatRangeAttributeValidationResolver.FLOAT_RANGE_EXCEPTION_CODE;

public class FloatRangeAttributeValidationResolverTest {


    @Test
    public void testResolve() {

        final String TEST_ATTRIBUTE = "test_attribute";
        final BigDecimal min = BigDecimal.valueOf(1.1);
        final BigDecimal max = BigDecimal.valueOf(5.5);
        final String wrongEntityValue = "1";
        final String entityValue = "1.1;5.5";
        final BigDecimal less = BigDecimal.valueOf(1);
        final BigDecimal in = BigDecimal.valueOf(2.2);
        final BigDecimal more = BigDecimal.valueOf(11.1111111000);
        Structure.Attribute wrongAttribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);
        Structure.Attribute attribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);

        //создание с неправильным типом типом атрибута
        try {
            new FloatRangeAttributeValidationResolver(wrongAttribute, min, max);
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
        FloatRangeValidationValue validationValue = new FloatRangeValidationValue().valueFromString(entityValue);
        FloatRangeAttributeValidationResolver resolver = new FloatRangeAttributeValidationResolver(attribute, validationValue);
        assertEquals(min, validationValue.getMin());
        assertEquals(max, validationValue.getMax());

        Message actual = resolver.resolve(less);
        assertEquals(FLOAT_RANGE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(more);
        assertEquals(FLOAT_RANGE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(in));
    }
}