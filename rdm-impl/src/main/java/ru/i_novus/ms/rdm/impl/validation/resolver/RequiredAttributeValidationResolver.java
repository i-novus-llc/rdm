package ru.i_novus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.ms.rdm.api.model.Structure;

/**
 * Проверка обязательности.
 */
public class RequiredAttributeValidationResolver implements AttributeValidationResolver {

    private static final String REQUIRED_FIELD_EXCEPTION_CODE = "validation.required.err";

    private final Structure.Attribute attribute;

    public RequiredAttributeValidationResolver(Structure.Attribute attribute) {
        this.attribute = attribute;
    }

    @Override
    public Message resolve(Object o) {
        return o == null || "".equals(o)
                ? new Message(REQUIRED_FIELD_EXCEPTION_CODE, attribute.getName())
                : null;
    }
}
