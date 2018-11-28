package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.IntRangeValidationValue;
import ru.inovus.ms.rdm.model.validation.PlainSizeValidationValue;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.inovus.ms.rdm.validation.resolver.IntRangeAttributeValidationResolver.INT_RANGE_EXCEPTION_CODE;

public class IntRangeAttributeValidationResolverTest {


    @Test
    public void testResolve() {
        final String TEST_ATTRIBUTE = "test_attribute";
        final BigInteger min = BigInteger.valueOf(-10);
        final BigInteger max = BigInteger.valueOf(5);
        final String wrongEntityValue = "-5";
        final String entityValue = "-10;5";
        final BigInteger less = BigInteger.valueOf(-11);
        final BigInteger in = BigInteger.valueOf(2);
        final BigInteger more = BigInteger.valueOf(11);
        final Structure.Attribute wrongAttribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
        final Structure.Attribute attribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);

        //создание с неправильным типом типом атрибута
        try {
            new IntRangeAttributeValidationResolver(wrongAttribute, min, max);
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }

        //создание validationValue с из неправильной строки
        try {
            new PlainSizeValidationValue().valueFromString(wrongEntityValue);
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }

        //проверка работы
        IntRangeValidationValue validationValue = new IntRangeValidationValue().valueFromString(entityValue);
        IntRangeAttributeValidationResolver resolver = new IntRangeAttributeValidationResolver(attribute, validationValue);
        assertEquals(min, validationValue.getMin());
        assertEquals(max, validationValue.getMax());

        Message actual = resolver.resolve(less);
        assertEquals(INT_RANGE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(more);
        assertEquals(INT_RANGE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(in));

    }
}