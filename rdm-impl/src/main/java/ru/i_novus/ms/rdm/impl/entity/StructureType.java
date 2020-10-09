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

        List<Structure.Attribute> attributes = new ArrayList<>();
        List<Structure.Reference> references = new ArrayList<>();

        if (attributesJson.isArray()) {
            for (JsonNode attributeJson : attributesJson) {

                Structure.Attribute attribute = createAttribute(attributeJson);
                Structure.Reference reference = createReference(attribute, attributeJson);

                if (reference != null) {
                    references.add(reference);
                }

                if (attribute != null) {
                    attributes.add(attribute);
                }
            }
        }

        return new Structure(attributes, references);
    }

    private Structure.Attribute createAttribute(JsonNode attributeJson) {

        String code = getByKey(attributeJson, "code", JsonNode::asText);
        if (isEmpty(code))
            return null;

        String name = getByKey(attributeJson, "name", JsonNode::asText);
        String type = getByKey(attributeJson, "type", JsonNode::asText);

        boolean isPrimary = Boolean.TRUE.equals(getByKey(attributeJson, "isPrimary", JsonNode::asBoolean));
        boolean localizable = Boolean.TRUE.equals(getByKey(attributeJson, "localizable", JsonNode::asBoolean));
        String description = getByKey(attributeJson, "description", JsonNode::asText);

        if (isPrimary)
            return Structure.Attribute.buildPrimary(code, name, FieldType.valueOf(type), description);

        if (localizable)
            return Structure.Attribute.buildLocalizable(code, name, FieldType.valueOf(type), description);

        return Structure.Attribute.build(code, name, FieldType.valueOf(type), description);
    }

    private Structure.Reference createReference(Structure.Attribute attribute, JsonNode attributeJson) {
        
        if (attribute == null || !attribute.isReferenceType())
            return null;

        String referenceCode = getByKey(attributeJson, "referenceCode", JsonNode::asText);
        String displayExpression = getByKey(attributeJson, "displayExpression", JsonNode::asText);

        return new Structure.Reference(attribute.getCode(), referenceCode, displayExpression);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws SQLException {

        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }

        try {
            ObjectNode structure = structureToJson((Structure) value);
            st.setObject(index, jsonMapper.writeValueAsString(structure), Types.OTHER);

        } catch (IOException ex) {
            throw new PersistenceException("Failed to convert Invoice to String: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode structureToJson(Structure structure) {

        ArrayNode attributesJson = jsonMapper.createArrayNode();
        structure.getAttributes().forEach(attribute ->
                attributesJson.add(createAttributeJson(attribute, structure.getReference(attribute.getCode())))
        );

        ObjectNode structureJson = jsonMapper.createObjectNode();
        structureJson.set("attributes", attributesJson);
        return structureJson;
    }

    private ObjectNode createAttributeJson(Structure.Attribute attribute, Structure.Reference reference) {

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
