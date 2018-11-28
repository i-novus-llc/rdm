package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.PlainSizeValidationValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.inovus.ms.rdm.validation.resolver.PlainSizeAttributeValidationResolver.PLAIN_SIZE_EXCEPTION_CODE;

public class PlainSizeAttributeValidationResolverTest {


    @Test
    public void testResolve() {
        final String TEST_ATTRIBUTE = "test_attribute";
        final int size = 5;
        final String wrongEntityValue = "-5";
        final String entityValue = "5";
        final int shortInt = 123;
        final int longInt = 123456789;
        final Structure.Attribute wrongAttribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
        final Structure.Attribute attribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.INTEGER, null);

        //создание с неправильным типом типом атрибута
        try {
            new PlainSizeAttributeValidationResolver(wrongAttribute, size);
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }

        //создание validationValue с из неправильной строки
        try {
            new PlainSizeValidationValue().valueFromString(wrongEntityValue);
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }

        PlainSizeValidationValue validationValue = new PlainSizeValidationValue().valueFromString(entityValue);
        PlainSizeAttributeValidationResolver resolver = new PlainSizeAttributeValidationResolver(attribute, validationValue);
        assertEquals(size, validationValue.getSize());

        //проверка работы
        Message actual = resolver.resolve(longInt);
        assertEquals(PLAIN_SIZE_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(shortInt));

    }
}