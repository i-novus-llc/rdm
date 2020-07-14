package ru.i_novus.ms.rdm.sync.model;


import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public enum DataTypeEnum {

    VARCHAR(asList("varchar", "text", "character varying")),
    INTEGER(asList("integer", "smallint", "bigint", "serial", "bigserial")),
    DATE(singletonList("date")),
    BOOLEAN(singletonList("boolean")),
    FLOAT(asList("decimal", "numeric")),
    JSONB(singletonList("jsonb"));

    private static final Map<String, DataTypeEnum> DATA_TYPE_FROM_STR = new HashMap<>();
    static {
        for (DataTypeEnum dt : DataTypeEnum.values()) {
            for (String s : dt.dataTypes)
                DATA_TYPE_FROM_STR.put(s, dt);
        }
    }

    private List<String> dataTypes;

    DataTypeEnum(List<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public List<String> getDataTypes() {
        return dataTypes;
    }

    @SuppressWarnings("squid:S1452")
    public List<?> castFromString(List<String> list) {
        switch (this) {
            case BOOLEAN: return list.stream().map(Boolean::valueOf).collect(toList());
            case INTEGER: return list.stream().map(BigInteger::new).collect(toList());
            case FLOAT: return list.stream().map(BigDecimal::new).collect(toList());
            case DATE: return list.stream().map(TimeUtils::parseLocalDate).collect(toList());
            case VARCHAR: return list;
            default:
                throw new RdmException("Cast from string to " + this.name() + " not supported.");
        }
    }

    public Object castFromString(String s) {
        return castFromString(singletonList(s)).get(0);
    }

    public static DataTypeEnum getByDataType(String dataType) {
        return dataType == null ? null : DATA_TYPE_FROM_STR.get(dataType);
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

