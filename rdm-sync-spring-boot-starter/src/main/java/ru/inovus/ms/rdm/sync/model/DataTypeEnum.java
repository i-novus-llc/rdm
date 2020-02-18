package ru.inovus.ms.rdm.sync.model;


import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.Structure;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public enum DataTypeEnum {
    VARCHAR(asList("varchar", "text", "character varying")),
    INTEGER(asList("integer", "smallint", "bigint", "serial", "bigserial")),
    DATE(singletonList("date")),
    BOOLEAN(singletonList("boolean")),
    FLOAT(asList("decimal", "numeric")),
    JSONB(singletonList("jsonb"));

    private List<String> dataTypes;

    DataTypeEnum(List<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public List<String> getDataTypes() {
        return dataTypes;
    }

    public static DataTypeEnum getByDataType(String dataType) {
        if (dataType == null)
            return null;
        for (DataTypeEnum value : values()) {
            if (value.getDataTypes().contains(dataType)) {
                return value;
            }
        }
        return null;
    }

    public static DataTypeEnum getByRdmAttr(Structure.Attribute attr) {
        switch (attr.getType()) {
            case DATE: return DATE;
            case FLOAT: return FLOAT;
            case INTEGER: return INTEGER;
            case STRING:
            case REFERENCE:
                return VARCHAR;
            case BOOLEAN: return BOOLEAN;
            default:
                throw new RdmException("Not supported field type: " + attr.getType());
        }
    }

}

