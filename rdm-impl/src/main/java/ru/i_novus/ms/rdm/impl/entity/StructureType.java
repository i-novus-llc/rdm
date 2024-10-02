package ru.i_novus.ms.rdm.impl.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.PersistenceException;
import org.apache.commons.lang3.SerializationUtils;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.function.Function;

import static ru.i_novus.ms.rdm.api.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.getMapper;

public class StructureType implements UserType<Structure> {

    @Override
    public int getSqlType() {
        return Types.JAVA_OBJECT;
    }

    @Override
    public Class<Structure> returnedClass() {
        return Structure.class;
    }

    @Override
    public boolean equals(Structure x, Structure y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Structure x) {
        return Objects.hashCode(x);
    }

    @Override
    public Structure nullSafeGet(ResultSet rs, int position,
                                 SharedSessionContractImplementor session,
                                 Object owner) throws SQLException {

        final String cellContent = rs.getString(position);
        if (cellContent == null)
            return null;

        try {
            JsonNode attributesJson = getMapper().readTree(cellContent).get("attributes");
            return jsonToStructure(attributesJson);

        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    private Structure jsonToStructure(JsonNode attributesJson) {

        Structure structure = new Structure();
        if (!attributesJson.isArray())
            return structure;

        for (JsonNode attributeJson : attributesJson) {

            Structure.Attribute attribute = createAttribute(attributeJson);
            Structure.Reference reference = createReference(attribute, attributeJson);
            structure.add(attribute, reference);
        }

        return structure;
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
    public void nullSafeSet(PreparedStatement st, Structure value, int index,
                            SharedSessionContractImplementor session) throws SQLException {

        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }

        try {
            ObjectNode structure = structureToJson(value);
            st.setObject(index, getMapper().writeValueAsString(structure), Types.OTHER);

        } catch (IOException ex) {
            throw new PersistenceException("Failed to convert Invoice to String: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode structureToJson(Structure structure) {

        ArrayNode attributesJson = getMapper().createArrayNode();
        structure.getAttributes().forEach(attribute ->
                attributesJson.add(createAttributeJson(attribute, structure.getReference(attribute.getCode())))
        );

        ObjectNode structureJson = getMapper().createObjectNode();
        structureJson.set("attributes", attributesJson);
        return structureJson;
    }

    private ObjectNode createAttributeJson(Structure.Attribute attribute, Structure.Reference reference) {

        ObjectNode attributeJson = getMapper().createObjectNode();
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
    public Structure deepCopy(Structure value) {

        return (value != null) ? SerializationUtils.clone(value) : null;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Structure value) {
        return this.deepCopy(value);
    }

    @Override
    public Structure assemble(Serializable cached, Object owner) {
        return deepCopy((Structure) cached);
    }

    @Override
    public Structure replace(Structure original, Structure target, Object owner) {
        return deepCopy(original);
    }

    private <T> T getByKey(JsonNode node, String key, Function<JsonNode, T> valueExtractor) {

        JsonNode jsonValue = node.get(key);
        return (jsonValue == null) ? null : valueExtractor.apply(jsonValue);
    }
}
