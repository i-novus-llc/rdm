package ru.i_novus.ms.rdm.impl.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.SerializationUtils;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.jsonMapper;

public class StructureType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.JAVA_OBJECT};
    }

    @Override
    public Class<? extends Structure> returnedClass() {
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
        if (cellContent == null)
            return null;

        try {
            JsonNode attributesJson = jsonMapper.readTree(cellContent).get("attributes");
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
                String type = getByKey(attributeJson, "type", JsonNode::asText);

                boolean isPrimary = Boolean.TRUE.equals(getByKey(attributeJson, "isPrimary", JsonNode::asBoolean));
                boolean localizable = Boolean.TRUE.equals(getByKey(attributeJson, "localizable", JsonNode::asBoolean));
                String description = getByKey(attributeJson, "description", JsonNode::asText);

                String referenceCode = getByKey(attributeJson, "referenceCode", JsonNode::asText);
                String displayExpression = getByKey(attributeJson, "displayExpression", JsonNode::asText);

                Structure.Attribute attribute;
                if (isPrimary) {
                    attribute = Structure.Attribute.buildPrimary(code, name, FieldType.valueOf(type), description);

                } else if (localizable) {
                    attribute = Structure.Attribute.buildLocalizable(code, name, FieldType.valueOf(type), description);

                } else {
                    attribute = Structure.Attribute.build(code, name, FieldType.valueOf(type), description);
                }

                if (attribute.isReferenceType()) {
                    Structure.Reference reference = new Structure.Reference(code, referenceCode, displayExpression);
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
            st.setObject(index, jsonMapper.writeValueAsString(structure), Types.OTHER);

        } catch (IOException ex) {
            throw new PersistenceException("Failed to convert Invoice to String: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode valueToJson(Object value) {

        Structure structure = (Structure) value;
        ArrayNode attributesJson = jsonMapper.createArrayNode();
        structure.getAttributes().forEach(attribute -> attributesJson.add(createAttributeJson(attribute, structure)));

        ObjectNode jsonStructure = jsonMapper.createObjectNode();
        jsonStructure.set("attributes", attributesJson);
        return jsonStructure;
    }

    private ObjectNode createAttributeJson(Structure.Attribute attribute, Structure structure) {

        ObjectNode attributeJson = jsonMapper.createObjectNode();
        attributeJson.put("code", attribute.getCode());
        attributeJson.put("name", attribute.getName());
        attributeJson.put("type", attribute.getType().name());

        if (attribute.hasIsPrimary()) {
            attributeJson.put("isPrimary", true);
        }
        if (attribute.isLocalizable()) {
            attributeJson.put("localizable", true);
        }
        if (!isEmpty(attribute.getDescription())) {
            attributeJson.put("description", attribute.getDescription());
        }

        Structure.Reference reference = structure.getReference(attribute.getCode());
        if (reference != null) {
            attributeJson.put("referenceCode", reference.getReferenceCode());
            if (reference.getDisplayExpression() != null)
                attributeJson.put("displayExpression", reference.getDisplayExpression());
        }

        return attributeJson;
    }

    @Override
    public Object deepCopy(Object value) {

        return (value != null) ? SerializationUtils.clone((Structure) value) : null;
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
        return (valueJson == null) ? null : valueExtractor.apply(valueJson);
    }
}
