package ru.inovus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.FloatRangeAttributeValidation;
import ru.inovus.ms.rdm.impl.validation.resolver.FloatRangeAttributeValidationResolver;

import java.math.BigDecimal;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.impl.validation.resolver.FloatRangeAttributeValidationResolver.FLOAT_RANGE_EXCEPTION_CODE;

public class FloatRangeAttributeValidationResolverTest {

    private final String TEST_ATTRIBUTE = "test_attribute";
    private final String wrongEntityValue = "1";
    private final String entityValue = "1.1;5.5";
    private final BigDecimal min = BigDecimal.valueOf(1.1);
    private final BigDecimal max = BigDecimal.valueOf(5.5);
    private final BigDecimal less = BigDecimal.valueOf(1);
    private final BigDecimal in = BigDecimal.valueOf(2.2);
    private final BigDecimal more = BigDecimal.valueOf(11.1111111000);
    private final Structure.Attribute wrongAttribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);
    private final Structure.Attribute attribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);

    /**
     * создание FloatRangeAttributeValidationResolver с неправильным типом атрибута
     */
    @Test
    public void testInvalidType() {
        try {
            new FloatRangeAttributeValidationResolver(wrongAttribute, min, max);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }
    }

    /**
     * заполнение FloatRangeAttributeValidation из строки
     */
    @Test
    public void testInvalidStringValue() {
        //из неправильной строки
        try {
            new FloatRangeAttributeValidation().valueFromString(wrongEntityValue);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }
        //из правильной строки
        FloatRangeAttributeValidation validation = new FloatRangeAttributeValidation().valueFromString(entityValue);
        assertEquals(min, validation.getMin());
        assertEquals(max, validation.getMax());
    }

    @Test
    public void testResolve() {
        FloatRangeAttributeValidationResolver resolver = new FloatRangeAttributeValidationResolver(attribute, min, max);
        Message actual = resolver.resolve(less);
        assertEquals(FLOAT_RANGE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(more);
        assertEquals(FLOAT_RANGE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(in));
    }
}