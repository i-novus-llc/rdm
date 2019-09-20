package ru.inovus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.RegExpAttributeValidation;

/**
 * Проверка сответствия регулярному выражению.
 */
public class RegExpAttributeValidationResolver implements AttributeValidationResolver<String> {


    public static final String REG_EXP_EXCEPTION_CODE = "validation.reg.exp.not.match";

    private final Structure.Attribute attribute;
    private final String regExp;

    public RegExpAttributeValidationResolver(Structure.Attribute attribute, String regExp) {
        if (!FieldType.STRING.equals(attribute.getType()))
            throw new UserException("attribute.validation.type.invalid");
        this.attribute = attribute;
        this.regExp = regExp;
    }

    public RegExpAttributeValidationResolver(Structure.Attribute attribute, RegExpAttributeValidation validationValue) {
        this(attribute, validationValue.getRegExp());
    }

    @Override
    public Message resolve(String value) {
        if (value == null || regExp == null || value.matches(regExp)) return null;
        else return new Message(REG_EXP_EXCEPTION_CODE, attribute.getName(), value, regExp);
    }
}
