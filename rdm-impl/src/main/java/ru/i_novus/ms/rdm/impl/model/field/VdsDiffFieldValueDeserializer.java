package ru.i_novus.ms.rdm.impl.model.field;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.*;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import static ru.i_novus.ms.rdm.api.util.TimeUtils.DATE_PATTERN_ISO_FORMATTER;

/**
 * Десериализатор DiffFieldValue для работы с vds.
 * <p/>
 * Обычная десериализация не восстанавливает oldValue и newValue в соответствии с типом поля.
 * Так как тип поля хранится внутри поля field, то @JsonTypeInfo не применим.
 * Так как не объявлены дочерние классы для DiffFieldValue, то @JsonSubTypes не применим.
 * Так как в БД уже есть сериализованные значения, то изменение (де)сериализации невозможно.
 * <p/>
 * См. также {@link ru.i_novus.ms.rdm.api.model.diff.DiffFieldValueDeserializer}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class VdsDiffFieldValueDeserializer extends StdDeserializer<DiffFieldValue> {

    public VdsDiffFieldValueDeserializer() {
        this(null);
    }

    public VdsDiffFieldValueDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public DiffFieldValue deserialize(JsonParser jsonParser, DeserializationContext ctxt)
            throws IOException {

        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);
        if (node == null)
            return null;

        JsonNode fieldNode = node.get("field");
        Field field = fieldNode != null ? codec.treeToValue(fieldNode, Field.class) : null;
        if (field == null)
            return null;

        JsonNode oldValueNode = node.get("oldValue");
        JsonNode newValueNode = node.get("newValue");
        JsonNode statusNode = node.get("status");

        return new DiffFieldValue(
                field,
                oldValueNode != null ? toFieldValue(field, oldValueNode.asText()) : null,
                newValueNode != null ? toFieldValue(field, newValueNode.asText()) : null,
                statusNode != null ? DiffStatusEnum.fromValue(node.get("status").asText()) : null
        );
    }

    /**
     * Преобразование сериализованного значения в значение, соответствующее типу поля.
     *
     * @param field поле
     * @param value сериализованное значение поля
     * @return Десериализованное значение поля
     */
    public static Serializable toFieldValue(Field field, String value) {

        if (value == null || StringUtils.isEmpty(value))
            return null;

        Class clazz = field.getFieldValueClass();
        return (clazz != null) ? toClassedFieldValue(clazz, value, field) : null;
    }

    /**
     * Преобразование сериализованного значения в значение в соответствии с классом значения поля.
     *
     * @param clazz класс значения поля
     * @param value сериализованное значение поля
     * @param field поле
     * @return Десериализованное значение поля
     */
    private static Serializable toClassedFieldValue(Class clazz, String value, Field field) {

        if (BooleanFieldValue.class.isAssignableFrom(clazz)) {
            return Boolean.valueOf(value);
        }

        if (DateFieldValue.class.isAssignableFrom(clazz)) {
            return LocalDate.parse(value, DATE_PATTERN_ISO_FORMATTER);
        }

        if (FloatFieldValue.class.isAssignableFrom(clazz)) {
            return new BigDecimal(value);
        }

        if (IntegerFieldValue.class.isAssignableFrom(clazz)) {
            return new BigInteger(value);
        }

        if (StringFieldValue.class.isAssignableFrom(clazz) ||
                IntegerStringFieldValue.class.isAssignableFrom(clazz)) {
            return value;
        }

        // NB: Строковые значения в составных полях!
        if (ReferenceFieldValue.class.isAssignableFrom(clazz) ||
                TreeFieldValue.class.isAssignableFrom(clazz)) {
            return value;
        }

        throw new RdmException(String.format("Unknown field value class for field: %s", field));
    }
}
