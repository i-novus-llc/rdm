package ru.inovus.ms.rdm.sync.model;


import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public enum DataTypeEnum {
    VARCHAR(asList("varchar", "text", "character varying")),
    INTEGER(asList("smallint", "integer", "bigint", "serial", "bigserial")),
    DATE(singletonList("date")),
    BOOLEAN(singletonList("boolean")),
    FLOAT(asList("numeric", "decimal")),
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
}

