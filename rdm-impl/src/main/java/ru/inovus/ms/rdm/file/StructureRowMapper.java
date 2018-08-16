package ru.inovus.ms.rdm.file;

import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


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
                                inputRow.getData().put(entry.getKey(), castValue(structure.getAttribute(entry.getKey()), (String) entry.getValue()))
                );
        return inputRow;
    }

    protected Object castValue(Structure.Attribute attribute, String value) {

        if (value == null || "".equals(value)) return null;

        switch (attribute.getType()) {
            case STRING:
                return value;
            case INTEGER:
                return BigInteger.valueOf(Long.parseLong(value));
            case FLOAT:
                return BigDecimal.valueOf(Double.parseDouble(value.replace(",", ".")));
            case DATE:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(value, formatter);
            case BOOLEAN:
                String lowerCase = value.toLowerCase();
                if (!"true".equals(lowerCase) && !"false".equals(lowerCase))
                    throw new RdmException("value is not boolean");
                return Boolean.valueOf(value);
            case REFERENCE:
                return createReference(attribute.getCode(), value);
            case TREE:
                return value;
            default:
                throw new RdmException("invalid type: " + attribute.getType());
        }
    }

    private Reference createReference(String attributeCode, String value) {
        Structure.Reference reference = structure.getReference(attributeCode);
        RefBookVersionEntity version = versionRepository.findOne(reference.getReferenceVersion());
        try {
            castValue(version.getStructure().getAttribute(reference.getReferenceAttribute()), value);
        } catch (Exception e) {
            throw new RdmException("reference value has a wrong type", e);
        }
        return new Reference(version.getStorageCode(), ConverterUtil.date(version.getFromDate()),
                reference.getReferenceAttribute(), reference.getDisplayAttributes().get(0), value);
    }
}
