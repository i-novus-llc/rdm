package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.Structure;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TypeValidation extends ErrorAttributeHolderValidation {

    private static final String VALIDATION_TYPE_EXCEPTION_CODE = "validation.type.error";
    private static final String ATTRIBUTE_TYPE_NOT_FOUND = "Type of attribute \"%1$s\" not found";
    private static final String ATTRIBUTE_TYPE_IS_INVALID = "Invalid type %1$s of attribute \"%2$s\"";

    private Map<String, Object> row;

    private Structure structure;

    public TypeValidation(Map<String, Object> row, Structure structure) {
        this.row = row;
        this.structure = structure;
    }

    @Override
    public List<Message> validate() {
        return row.entrySet().stream()
                .filter(entry -> !isErrorAttribute(entry.getKey()))
                .map(this::validate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Message validate(Map.Entry<String, Object> entry) {

        Structure.Attribute attribute = structure.getAttribute(entry.getKey());
        if (attribute == null)
            return null;

        Message message = validateType(attribute, entry.getValue());
        if (message == null)
            return null;

        addErrorAttribute(entry.getKey());

        return message;
    }

    public static Message validateType(Structure.Attribute attribute, Object value) {

        return validateInstance(attribute.getCode(), value, toClass(attribute));
    }

    private static Class toClass(Structure.Attribute attribute) {

        FieldType type = attribute.getType();
        if (type == null)
            throw new RdmException(String.format(ATTRIBUTE_TYPE_NOT_FOUND, attribute.getName()));

        switch (type) {
            case STRING:
            case TREE: return String.class;
            case INTEGER: return BigInteger.class;
            case FLOAT: return BigDecimal.class;
            case DATE: return LocalDate.class;
            case BOOLEAN: return Boolean.class;
            case REFERENCE: return Reference.class;
            default: throw new RdmException(String.format(ATTRIBUTE_TYPE_IS_INVALID, type.name(), attribute.getName()));
        }
    }

    private static Message validateInstance(String name, Object value, Class clazz) {

        if (value == null || clazz.isInstance(value))
            return null;

        return new Message(VALIDATION_TYPE_EXCEPTION_CODE, name, value);
    }
}
