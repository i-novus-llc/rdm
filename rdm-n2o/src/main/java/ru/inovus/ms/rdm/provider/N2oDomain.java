package ru.inovus.ms.rdm.provider;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

public class N2oDomain {

    public static final String STRING = "string";
    public static final String INTEGER = "integer";
    public static final String FLOAT = "numeric";
    public static final String DATE = "date";
    public static final String BOOLEAN = "boolean";

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
