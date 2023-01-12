package ru.i_novus.ms.rdm.api.model.diff;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.field.CommonField;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import static ru.i_novus.ms.rdm.api.util.TimeUtils.DATE_PATTERN_ISO_FORMATTER;

/**
 * Десериализатор DiffFieldValue для rdm API.
 * <p/>
 * Обычная десериализация не восстанавливает oldValue и newValue в соответствии с типом поля.
 * Так как тип поля не восстанавливается для CommonField, то @JsonTypeInfo не применим.
 * Так как в БД уже есть сериализованные значения, то изменение (де)сериализации невозможно.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DiffFieldValueDeserializer extends StdDeserializer<DiffFieldValue> {

    public DiffFieldValueDeserializer() {
        this(null);
    }

    public DiffFieldValueDeserializer(Class<?> vc) {
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

        String fieldId = null;
        if (field instanceof CommonField) {
            JsonNode idNode = fieldNode.get("id");
            fieldId = idNode != null ? idNode.asText() : null;
        }

        JsonNode oldValueNode = node.get("oldValue");
        JsonNode newValueNode = node.get("newValue");
        JsonNode statusNode = node.get("status");

        return new DiffFieldValue(
                field,
                oldValueNode != null ? toFieldValue(field, fieldId, oldValueNode.asText()) : null,
                newValueNode != null ? toFieldValue(field, fieldId, newValueNode.asText()) : null,
                statusNode != null ? DiffStatusEnum.fromValue(node.get("status").asText()) : null
        );
    }

    /**
     * Преобразование сериализованного значения в значение, соответствующее типу поля.
     *
     * @param field   поле
     * @param fieldId идентификатор поля (в случае CommonField)
     * @param value   сериализованное значение поля
     * @return Десериализованное значение поля
     */
    public static Serializable toFieldValue(Field field, String fieldId, String value) {

        if (value == null || StringUtils.isEmpty(value))
            return null;

        return toCommonFieldValue(fieldId, value, field);
    }

    /**
     * Преобразование сериализованного значения в значение в соответствии с идентификатором поля.
     *
     * @param id    идентификатор поля CommonField
     * @param value сериализованное значение поля
     * @param field поле
     * @return Десериализованное значение поля
     */
    private static Serializable toCommonFieldValue(String id, String value, Field field) {

        if (id == null)
            throw new RdmException(String.format("Absent field identifier for field: %s", field));

        return switch (id) {
            case "BooleanField" -> Boolean.valueOf(value);
            case "DateField" -> LocalDate.parse(value, DATE_PATTERN_ISO_FORMATTER);
            case "FloatField" -> new BigDecimal(value);
            case "IntegerField" -> new BigInteger(value);
            case "StringField",
                    "IntegerStringField" -> value;

            // NB: Строковые значения в составных полях!
            case "ReferenceField",
                    "TreeField" -> value;
            default ->
                    throw new RdmException(String.format("Unknown field identifier for field: %s", field));
        };
    }
}
