package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.validation.PlainSizeAttributeValidation;

import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.INTEGER;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.STRING;

/**
 * Простая проверка дины.
 * В value хранится размер
 */
public class PlainSizeAttributeValidationResolver implements AttributeValidationResolver {

    public static final String PLAIN_SIZE_EXCEPTION_CODE = "validation.plain.size.err";
    private final Structure.Attribute attribute;
    private final int size;

    public PlainSizeAttributeValidationResolver(Structure.Attribute attribute, int size) {
        if (!STRING.equals(attribute.getType()) && !INTEGER.equals(attribute.getType()))
            throw new UserException("attribute.validation.type.invalid");
        this.attribute = attribute;
        this.size = size;
    }

    public PlainSizeAttributeValidationResolver(Structure.Attribute attribute, PlainSizeAttributeValidation validationValue) {
        this(attribute, validationValue.getSize());
    }

    @Override
    public Message resolve(Object o) {
        return o != null && String.valueOf(o).length() > size
                ? new Message(PLAIN_SIZE_EXCEPTION_CODE, attribute.getName(), o, size)
                : null;
    }
}
