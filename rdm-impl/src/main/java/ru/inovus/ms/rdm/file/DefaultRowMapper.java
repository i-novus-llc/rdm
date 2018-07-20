package ru.inovus.ms.rdm.file;

import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.exception.NsiException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by tnurdinov on 06.07.2018.
 */
public class DefaultRowMapper implements RowMapper {

    private Structure structure;

    private RefBookVersionRepository versionRepository;

    public DefaultRowMapper(Structure structure, RefBookVersionRepository versionRepository) {
        this.structure = structure;
        this.versionRepository = versionRepository;
    }

    @Override
    public Row map(Row inputRow) {
        inputRow.getData().forEach((name, value) ->
            inputRow.getData().put(name, castValue(structure.getAttribute(name), (String) value))
        );
        return inputRow;
    }

    public Object castValue(Structure.Attribute attribute, String value) {
        switch (attribute.getType()) {
            case STRING:
                return value;
            case INTEGER:
                return Integer.parseInt(value);
            case FLOAT:
                return Float.parseFloat(value);
            case DATE:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(value, formatter);
            case BOOLEAN:
                return Boolean.valueOf(value);
            case REFERENCE:
                return createReference(attribute.getCode(), value);
            case TREE:
                return value;
            default:
                throw new NsiException("invalid type: " + attribute.getType());
        }
    }

    private Reference createReference(String attributeCode, String value) {
        Structure.Reference reference = structure.getReference(attributeCode);
        RefBookVersionEntity version = versionRepository.findOne(reference.getReferenceVersion());
        version.getStructure().getAttribute(reference.getReferenceAttribute());
        return new Reference(version.getStorageCode(), ConverterUtil.date(version.getFromDate()),
                reference.getReferenceAttribute(), reference.getDisplayAttributes().get(0), value);
    }
}
