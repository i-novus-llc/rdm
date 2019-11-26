package ru.inovus.ms.rdm.impl.file;

import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.math.BigDecimal;
import java.math.BigInteger;

import static ru.inovus.ms.rdm.api.util.TimeUtils.parseLocalDate;

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
                value);
    }
}
