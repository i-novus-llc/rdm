package ru.inovus.ms.rdm.provider;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

public class N2oDomain {
    static final String STRING = "string";
    static final String INTEGER = "integer";
    static final String FLOAT = "numeric";
    static final String DATE = "date";
    static final String BOOLEAN = "boolean";

    private N2oDomain() {
    }

    static String fieldTypeToDomain(FieldType fieldType) {
        switch (fieldType) {
            case STRING: return N2oDomain.STRING;
            case INTEGER: return N2oDomain.INTEGER;
            case FLOAT: return N2oDomain.FLOAT;
            case DATE: return N2oDomain.DATE;
            case BOOLEAN: return N2oDomain.BOOLEAN;
            default:
                throw new IllegalArgumentException("field type not supported");
        }
    }
}
