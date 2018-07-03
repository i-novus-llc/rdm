package ru.inovus.ms.rdm.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.*;
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
        return x == y || x.equals(y);
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
                String name = getByKey(attributeJson, "attributeName", JsonNode::asText);
                String type = getByKey(attributeJson, "type", JsonNode::asText);
                boolean isPrimary = getByKey(attributeJson, "isPrimary", JsonNode::asBoolean);
                boolean isRequired = getByKey(attributeJson, "isRequired", JsonNode::asBoolean);
                Integer referenceVersion = getByKey(attributeJson, "referenceVersion", JsonNode::asInt);
                String referenceAttribute = getByKey(attributeJson, "referenceAttribute", JsonNode::asText);
                Function<JsonNode, List<String>> asList = jsonNode -> {
                    List<String> values = new ArrayList<>();
                    ArrayNode arrayNode = ((ArrayNode) jsonNode);
                    arrayNode.forEach(node -> values.add(node.asText()));
                    return values;
                };
                List<String> displayAttributes = getByKey(attributeJson, "displayAttribute", asList);
                Structure.Attribute attribute;
                if (isPrimary) {
                    attribute = Structure.Attribute.buildPrimary(name, FieldType.valueOf(type));
                } else {
                    attribute = Structure.Attribute.build(name, FieldType.valueOf(type), isRequired);
                }
                if (FieldType.valueOf(type).equals(FieldType.REFERENCE)) {
                    Structure.Reference reference = new Structure.Reference(name, referenceVersion, referenceAttribute, displayAttributes);
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
        attributeJson.put("attributeName", attribute.getAttributeName());
        attributeJson.put("type", attribute.getType().name());
        attributeJson.put("isPrimary", attribute.isPrimary());
        attributeJson.put("isRequired", attribute.isRequired());
        Structure.Reference reference = structure.getReference(attribute.getAttributeName());
        if (reference != null) {
            attributeJson.put("referenceVersion", reference.getReferenceVersion());
            attributeJson.put("referenceAttribute", reference.getReferenceAttribute());
            ArrayNode arrayNode = attributeJson.putArray("displayFields");
            Optional.ofNullable(reference.getDisplayAttributes()).ifPresent(d -> d.forEach(arrayNode::add));
        }

        return attributeJson;
    }

    @Override
    public Object deepCopy(Object value) {
        Object copy = null;
        if (value != null) {
            copy = jsonToStructure(valueToJson(value));
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
