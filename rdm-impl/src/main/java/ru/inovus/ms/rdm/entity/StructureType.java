package ru.inovus.ms.rdm.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.SerializationUtils;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Structure;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class StructureType implements UserType {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.JAVA_OBJECT};
    }

    @Override
    public Class returnedClass() {
        return Structure.class;
    }

    @Override
    public boolean equals(Object x, Object y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) {
        return Objects.hashCode(x);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException {
        final String cellContent = rs.getString(names[0]);
        if (cellContent == null) {
            return null;
        }

        try {
            JsonNode attributesJson = MAPPER.readTree(cellContent).get("attributes");
            return jsonToStructure(attributesJson);
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    private Structure jsonToStructure(JsonNode attributesJson) {
        Structure structure = new Structure();
        List<Structure.Attribute> attributes = new ArrayList<>();
        List<Structure.Reference> references = new ArrayList<>();
        if (attributesJson.isArray()) {
            for (JsonNode attributeJson : attributesJson) {
                String code = getByKey(attributeJson, "code", JsonNode::asText);
                String name = getByKey(attributeJson, "name", JsonNode::asText);
                String description = getByKey(attributeJson, "description", JsonNode::asText);
                String type = getByKey(attributeJson, "type", JsonNode::asText);
                boolean isPrimary = getByKey(attributeJson, "isPrimary", JsonNode::asBoolean);
                Integer referenceVersion = getByKey(attributeJson, "referenceVersion", JsonNode::asInt);
                String referenceAttribute = getByKey(attributeJson, "referenceAttribute", JsonNode::asText);
                String displayExpression = getByKey(attributeJson, "referenceDisplayExpression", JsonNode::asText);
                Structure.Attribute attribute;
                if (isPrimary) {
                    attribute = Structure.Attribute.buildPrimary(code, name, FieldType.valueOf(type), description);
                } else {
                    attribute = Structure.Attribute.build(code, name, FieldType.valueOf(type), description);
                }
                if (FieldType.valueOf(type).equals(FieldType.REFERENCE)) {
                    Structure.Reference reference = new Structure.Reference(code, referenceVersion, referenceAttribute, displayExpression);
                    references.add(reference);
                }
                attributes.add(attribute);
            }
        }
        structure.setAttributes(attributes);
        structure.setReferences(references);
        return structure;
    }


    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        try {
            ObjectNode structure = valueToJson(value);
            st.setObject(index, MAPPER.writeValueAsString(structure), Types.OTHER);
        } catch (IOException ex) {
            throw new PersistenceException("Failed to convert Invoice to String: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode valueToJson(Object value) {
        Structure structure = (Structure) value;
        ArrayNode attributesJson = MAPPER.createArrayNode();
        structure.getAttributes().forEach(attribute -> attributesJson.add(createAttributeJson(attribute, structure)));
        ObjectNode jsonStructure = MAPPER.createObjectNode();
        jsonStructure.set("attributes", attributesJson);
        return jsonStructure;
    }

    private ObjectNode createAttributeJson(Structure.Attribute attribute, Structure structure) {
        ObjectNode attributeJson = MAPPER.createObjectNode();
        attributeJson.put("code", attribute.getCode());
        attributeJson.put("name", attribute.getName());
        attributeJson.put("type", attribute.getType().name());
        attributeJson.put("isPrimary", attribute.getIsPrimary());
        Optional.ofNullable(attribute.getDescription()).ifPresent(d -> attributeJson.put("description", attribute.getDescription()));
        Structure.Reference reference = structure.getReference(attribute.getCode());
        if (reference != null) {
            attributeJson.put("referenceVersion", reference.getReferenceVersion());
            attributeJson.put("referenceAttribute", reference.getReferenceAttribute());
            if (reference.getDisplayExpression() != null)
                attributeJson.put("referenceDisplayExpression", reference.getDisplayExpression());
        }

        return attributeJson;
    }

    @Override
    public Object deepCopy(Object value) {
        Object copy = null;
        if (value != null) {
            copy = SerializationUtils.clone((Structure) value);
        }
        return copy;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) {
        return (Serializable) this.deepCopy(value);
    }

    @Override
    public Object assemble(Serializable cached, Object owner) {
        return deepCopy(cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner) {
        return deepCopy(original);
    }

    private <T> T getByKey(JsonNode node, String key, Function<JsonNode, T> valueExtractor) {
        JsonNode valueJson = node.get(key);
        if (valueJson == null) {
            return null;
        }
        return valueExtractor.apply(valueJson);
    }
}
