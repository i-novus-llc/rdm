package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.FloatSizeValidationValue;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.inovus.ms.rdm.validation.resolver.FloatSizeAttributeValidationResolver.FLOAT_FRAC_SIZE_EXCEPTION_CODE;
import static ru.inovus.ms.rdm.validation.resolver.FloatSizeAttributeValidationResolver.FLOAT_INT_SIZE_EXCEPTION_CODE;

public class FloatSizeAttributeValidationResolverTest {

    @Test
    public void testResolve() {
        final String TEST_ATTRIBUTE = "test_attribute";
        final int intPartSize = 2;
        final int fracPartSize = 3;
        final String wrongEntityValue = "2";
        final String entityValue = "2;3";
        final BigDecimal longInt = BigDecimal.valueOf(123.123);
        final BigDecimal longFrac = BigDecimal.valueOf(23.1234);
        final BigDecimal testValue = BigDecimal.valueOf(23.123);
        final Structure.Attribute wrongAttribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);
        final Structure.Attribute attribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);

        //создание с неправильным типом типом атрибута
        try {
            new FloatSizeAttributeValidationResolver(wrongAttribute, intPartSize, fracPartSize);
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }

        //создание validationValue с из неправильной строки
        try {
            new FloatSizeValidationValue().valueFromString(wrongEntityValue);
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }

        FloatSizeValidationValue validationValue = new FloatSizeValidationValue().valueFromString(entityValue);
        FloatSizeAttributeValidationResolver resolver = new FloatSizeAttributeValidationResolver(attribute, validationValue);
        assertEquals(intPartSize, validationValue.getIntPartSize());
        assertEquals(fracPartSize, validationValue.getFracPartSize());

        //проверка работы
        Message actual = resolver.resolve(longInt);
        assertEquals(FLOAT_INT_SIZE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(longFrac);
        assertEquals(FLOAT_FRAC_SIZE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(testValue));

    }
}