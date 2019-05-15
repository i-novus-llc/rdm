package ru.inovus.ms.rdm.file;

import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.math.BigDecimal;
import java.math.BigInteger;

import static ru.inovus.ms.rdm.util.TimeUtils.parseLocalDate;

public class StructureRowMapper implements RowMapper {

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

        if (attribute == null || value == null || "".equals(String.valueOf(value).trim()))
            return null;

        switch (attribute.getType()) {
            case STRING:
                return String.valueOf(value);
            case INTEGER:
                if (value instanceof BigInteger)
                    return value;
                return BigInteger.valueOf(Long.parseLong(String.valueOf(value)));
            case FLOAT:
                if (value instanceof BigDecimal)
                    return value;
                return new BigDecimal(String.valueOf(value).replace(",", "."));
            case DATE:
                return parseLocalDate(value);
            case BOOLEAN:
                if (value instanceof Boolean)
                    return value;
                String lowerCase = String.valueOf(value).toLowerCase();
                if (!"true".equals(lowerCase) && !"false".equals(lowerCase))
                    throw new RdmException("value is not boolean");
                return Boolean.valueOf(lowerCase);
            case REFERENCE:
                if (value instanceof Reference)
                    return value;
                return createReference(attribute.getCode(), String.valueOf(value));
            case TREE:
                return value;
            default:
                throw new RdmException("invalid type: " + attribute.getType());
        }
    }

    private Reference createReference(String attributeCode, String value) {
        Structure.Reference reference = structure.getReference(attributeCode);
        RefBookVersionEntity version = versionRepository
                .findById(reference.getReferenceVersion())
                .orElseThrow(() -> new RdmException("referenced version does not exist: " + reference.getReferenceVersion()));
        try {
            castValue(version.getStructure().getAttribute(reference.getReferenceAttribute()), value);
        } catch (Exception e) {
            throw new RdmException("reference value has a wrong type", e);
        }
        return new Reference(
                version.getStorageCode(),
                version.getFromDate(),
                reference.getReferenceAttribute(),
                new DisplayExpression(reference.getDisplayExpression()),
                value);
    }
}
