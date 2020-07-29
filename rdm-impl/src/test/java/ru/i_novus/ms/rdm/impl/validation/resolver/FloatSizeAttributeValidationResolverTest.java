package ru.i_novus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.validation.FloatSizeAttributeValidation;

import java.math.BigDecimal;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.impl.validation.resolver.FloatSizeAttributeValidationResolver.FLOAT_FRAC_SIZE_EXCEPTION_CODE;
import static ru.i_novus.ms.rdm.impl.validation.resolver.FloatSizeAttributeValidationResolver.FLOAT_INT_SIZE_EXCEPTION_CODE;

public class FloatSizeAttributeValidationResolverTest {
    private final String TEST_ATTRIBUTE = "test_attribute";
    private final String wrongEntityValue = "2";
    private final String entityValue = "2;3";
    private final int intPartSize = 2;
    private final int fracPartSize = 3;
    private final BigDecimal longInt = BigDecimal.valueOf(123.123);
    private final BigDecimal longFrac = BigDecimal.valueOf(23.1234);
    private final BigDecimal testValue = BigDecimal.valueOf(23.123);
    private final Structure.Attribute wrongAttribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);
    private final Structure.Attribute attribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);

    /**
     * создание FloatSizeAttributeValidationResolver с неправильным типом атрибута
     */
    @Test
    public void testInvalidType() {
        try {
            new FloatSizeAttributeValidationResolver(wrongAttribute, intPartSize, fracPartSize);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }
    }

    /**
     * заполнение FloatSizeAttributeValidation из строки
     */
    @Test
    public void testInvalidStringValue() {
        //из неправильной строки
        try {
            new FloatSizeAttributeValidation().valueFromString(wrongEntityValue);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }
        //из правильной строки
        FloatSizeAttributeValidation validation = new FloatSizeAttributeValidation().valueFromString(entityValue);
        assertEquals(intPartSize, validation.getIntPartSize());
        assertEquals(fracPartSize, validation.getFracPartSize());
    }

    @Test
    public void testResolve() {
        FloatSizeAttributeValidationResolver resolver = new FloatSizeAttributeValidationResolver(attribute, intPartSize, fracPartSize);
        Message actual = resolver.resolve(longInt);
        assertEquals(FLOAT_INT_SIZE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(longFrac);
        assertEquals(FLOAT_FRAC_SIZE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(testValue));

    }
}