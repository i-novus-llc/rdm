package ru.i_novus.ms.rdm.impl.util.mappers;

import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.row.RowMapper;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.math.BigDecimal;
import java.math.BigInteger;

import static ru.i_novus.ms.rdm.api.util.TimeUtils.parseLocalDate;

public class StructureRowMapper implements RowMapper {

    private static final String BOOLEAN_LOWER_TRUE = "true";
    private static final String BOOLEAN_LOWER_FALSE = "false";
    private static final String BOOLEAN_VALUE_BRACES = "()";

    protected Structure structure;

    private RefBookVersionRepository versionRepository;

    public StructureRowMapper(Structure structure, RefBookVersionRepository versionRepository) {
        this.structure = structure;
        this.versionRepository = versionRepository;
    }

    @Override
    public Row map(Row inputRow) {
        inputRow.getData().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry ->
                        inputRow.getData().put(entry.getKey(),
                                castValue(structure.getAttribute(entry.getKey()), entry.getValue()))
                );
        return inputRow;
    }

    protected Object castValue(Structure.Attribute attribute, Object value) {

        if (attribute == null || value == null || value.toString().isBlank())
            return null;

        switch (attribute.getType()) {
            case STRING:
                return value.toString();

            case INTEGER:
                if (value instanceof BigInteger)
                    return value;
                return new BigInteger(value.toString());

            case FLOAT:
                if (value instanceof BigDecimal)
                    return value;
                return new BigDecimal(value.toString().replace(",", "."));

            case DATE:
                return parseLocalDate(value);

            case BOOLEAN:
                return parseBoolean(value);

            case REFERENCE:
                if (value instanceof Reference)
                    return value;
                return createReference(attribute.getCode(), value.toString());

            case TREE:
                return value;

            default:
                throw new RdmException("Unexpected type: " + attribute.getType());
        }
    }

    private Boolean parseBoolean(Object value) {

        if (value instanceof Boolean)
            return (Boolean) value;

        String stringValue = String.valueOf(value).trim().toLowerCase();
        if (stringValue.endsWith(BOOLEAN_VALUE_BRACES)) {
            stringValue = stringValue.substring(0, stringValue.length() - BOOLEAN_VALUE_BRACES.length());
        }

        if (!BOOLEAN_LOWER_TRUE.equals(stringValue)
                && !BOOLEAN_LOWER_FALSE.equals(stringValue))
            throw new RdmException("Value is not of boolean type: " + stringValue);

        return Boolean.valueOf(stringValue);
    }

    private Reference createReference(String attributeCode, String value) {

        Structure.Reference reference = structure.getReference(attributeCode);
        RefBookVersionEntity version = versionRepository
                .findFirstByRefBookCodeAndStatusOrderByFromDateDesc(reference.getReferenceCode(), RefBookVersionStatus.PUBLISHED);
        if (version == null)
            throw new RdmException("version.not.found");

        Structure.Attribute referenceAttribute = reference.findReferenceAttribute(version.getStructure());
        try {
            castValue(referenceAttribute, value);

        } catch (Exception e) {
            throw new RdmException("reference value has a wrong type", e);
        }

        return new Reference(
                version.getStorageCode(),
                version.getFromDate(),
                referenceAttribute.getCode(),
                new DisplayExpression(reference.getDisplayExpression()),
                value
        );
    }
}
