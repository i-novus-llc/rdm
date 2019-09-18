package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.model.validation.PlainSizeAttributeValidation;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.validation.resolver.PlainSizeAttributeValidationResolver.PLAIN_SIZE_EXCEPTION_CODE;

public class PlainSizeAttributeValidationResolverTest {

    private final String TEST_ATTRIBUTE = "test_attribute";
    private final String wrongEntityValue = "-5";
    private final String entityValue = "5";
    private final int size = 5;
    private final int shortInt = 123;
    private final int longInt = 123456789;
    private final Structure.Attribute wrongAttribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
    private final Structure.Attribute attribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);

    /**
     * создание PlainSizeAttributeValidationResolver с неправильным типом атрибута
     */
    @Test
    public void testInvalidType() {
        try {
            new PlainSizeAttributeValidationResolver(wrongAttribute, size);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }
    }

    /**
     * заполнение PlainSizeAttributeValidation из строки
     */
    @Test
    public void testInvalidStringValue() {
        //из неправильной строки
        try {
            new PlainSizeAttributeValidation().valueFromString(wrongEntityValue);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }
        //из правильной строки
        PlainSizeAttributeValidation validationValue = new PlainSizeAttributeValidation().valueFromString(entityValue);
        assertEquals(size, validationValue.getSize());
    }

    @Test
    public void testResolve() {
        PlainSizeAttributeValidationResolver resolver = new PlainSizeAttributeValidationResolver(attribute, size);
        Message actual = resolver.resolve(longInt);
        assertEquals(PLAIN_SIZE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(shortInt));

    }
}