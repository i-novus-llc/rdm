package ru.inovus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.RegExpAttributeValidation;
import ru.inovus.ms.rdm.impl.validation.resolver.RegExpAttributeValidationResolver;

import static org.junit.Assert.*;
import static ru.inovus.ms.rdm.impl.validation.resolver.RegExpAttributeValidationResolver.REG_EXP_EXCEPTION_CODE;

public class RegExpAttributeValidationResolverTest {

    private final String TEST_ATTRIBUTE = "test_attribute";
    private final String wrongEntityValue = "(";
    private final String regExp = "^.{1}$";
    private final String matchString = "a";
    private final String notMatchString = "aa";
    private final Structure.Attribute wrongAttribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.FLOAT, null);
    private final Structure.Attribute attribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.STRING, null);

    /**
     * создание RegExpAttributeValidationResolver с неправильным типом атрибута
     */
    @Test
    public void testInvalidType() {
        try {
            new RegExpAttributeValidationResolver(wrongAttribute, regExp);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.type.invalid", e.getCode());
        }
    }

    /**
     * заполнение RegExpAttributeValidation из строки
     */
    @Test
    public void testInvalidStringValue() {
        //из неправильной строки
        try {
            new RegExpAttributeValidation().valueFromString(wrongEntityValue);
            fail();
        } catch (UserException e) {
            assertEquals("attribute.validation.reg.exp.invalid", e.getCode());
        }
        //из правильной строки
        RegExpAttributeValidation validation = new RegExpAttributeValidation().valueFromString(regExp);
        assertEquals(regExp, validation.getRegExp());
    }

    @Test
    public void testResolve() {
        RegExpAttributeValidationResolver resolver = new RegExpAttributeValidationResolver(attribute, regExp);
        Message actual = resolver.resolve(notMatchString);
        assertEquals(REG_EXP_EXCEPTION_CODE, actual.getCode());
        assertNull(resolver.resolve(matchString));

    }
}