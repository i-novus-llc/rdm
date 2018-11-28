package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.RegExpValidationValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.inovus.ms.rdm.validation.resolver.RegExpAttributeValidationResolver.REG_EXP_EXCEPTION_CODE;

public class RegExpAttributeValidationResolverTest {

    @Test
    public void testResolve() {
        final String TEST_ATTRIBUTE = "test_attribute";
        final int size = 5;
        final String wrongEntityValue = "(";
        final String entityValue = "^.{1}$";
        final String matchString = "a";
        final String notMatchString = "aa";
        final Structure.Attribute wrongAttribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
        final Structure.Attribute attribute =
                Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.STRING, null);

        //создание с неправильным типом типом атрибута
        try {
            new RegExpAttributeValidationResolver(wrongAttribute, "");
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }

        //создание validationValue с из неправильной строки
        try {
            new RegExpValidationValue().valueFromString(wrongEntityValue);
        } catch (UserException e) {
            assertEquals("attribute.validation.value.invalid", e.getCode());
        }

        RegExpValidationValue validationValue = new RegExpValidationValue().valueFromString(entityValue);
        RegExpAttributeValidationResolver resolver = new RegExpAttributeValidationResolver(attribute, validationValue);

        //проверка работы
        Message actual = resolver.resolve(notMatchString);
        assertEquals(REG_EXP_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(matchString));

    }
}