package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.model.validation.IntRangeAttributeValidation;

import java.math.BigInteger;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.validation.resolver.IntRangeAttributeValidationResolver.INT_RANGE_EXCEPTION_CODE;

public class IntRangeAttributeValidationResolverTest {

    private final String TEST_ATTRIBUTE = "test_attribute";
    private final String wrongEntityValue = "-5";
    private final String entityValue = "-10;5";
    private final BigInteger min = BigInteger.valueOf(-10);
    private final BigInteger max = BigInteger.valueOf(5);
    private final BigInteger less = BigInteger.valueOf(-11);
    private final BigInteger in = BigInteger.valueOf(2);
    private final BigInteger more = BigInteger.valueOf(11);
    private final Structure.Attribute wrongAttribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
    private final Structure.Attribute attribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);

    /**
     * создание IntRangeAttributeValidationResolver с неправильным типом атрибута
     */
    @Test
    public void testInvalidType() {
        try {
            new IntRangeAttributeValidationResolver(wrongAttribute, min, max);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }
    }

    /**
     * заполнение IntRangeAttributeValidation из строки
     */
    @Test
    public void testInvalidStringValue() {
        //из неправильной строки
        try {
            new IntRangeAttributeValidation().valueFromString(wrongEntityValue);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }
        //из правильной строки
        IntRangeAttributeValidation validation = new IntRangeAttributeValidation().valueFromString(entityValue);
        assertEquals(min, validation.getMin());
        assertEquals(max, validation.getMax());
    }

    @Test
    public void testResolve() {
        IntRangeAttributeValidationResolver resolver = new IntRangeAttributeValidationResolver(attribute, min, max);
        Message actual = resolver.resolve(less);
        assertEquals(INT_RANGE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(more);
        assertEquals(INT_RANGE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(in));

    }
}