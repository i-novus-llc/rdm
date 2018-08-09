package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Structure;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TypeValidation implements RdmValidation {

    private static final String VALIDATION_TYPE_EXCEPTION_CODE = "validation.type.error";

    private Map<String, Object> row;

    private Structure structure;

    public TypeValidation(Map<String, Object> row, Structure structure) {
        this.row = row;
        this.structure = structure;
    }

    @Override
    public List<Message> validate() {
        List<Message> messages = new ArrayList<>();
        row.forEach((name, value) -> {
            FieldType type = structure.getAttribute(name).getType();
            Optional.ofNullable(checkType(type, name, value)).ifPresent(messages::add);
        });
        return messages;
    }

    private Message checkType(FieldType type, String name, Object value) {
        switch (type) {
            case STRING:
                return checkInstance(name, value, String.class);
            case INTEGER:
                return checkInstance(name, value, BigInteger.class);
            case FLOAT:
                return checkInstance(name, value, Float.class);
            case DATE:
                return checkInstance(name, value, LocalDate.class);
            case BOOLEAN:
                return checkInstance(name, value, Boolean.class);
            case REFERENCE:
                return checkInstance(name, value, Reference.class);
            case TREE:
                return checkInstance(name, value, String.class);
            default:
                throw new RdmException("invalid type: " + type);
        }
    }

    private Message checkInstance(String name, Object value, Class cls) {
        if (!(cls.isInstance(value))) {
            return new Message(VALIDATION_TYPE_EXCEPTION_CODE, name, value);
        }
        return null;
    }
}
