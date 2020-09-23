package ru.i_novus.ms.rdm.n2o.api.constant;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

/**
 * Типы данных N2O в соответствии с типами атрибутов в RDM.
 *
 * @see net.n2oapp.framework.api.metadata.domain.Domain
 */
public class N2oDomain {

    public static final String STRING = "string";
    public static final String INTEGER = "integer";
    public static final String LONG = "long";
    public static final String FLOAT = "numeric";
    public static final String LOCALDATE = "localdate";
    public static final String BOOLEAN = "boolean";

    private N2oDomain() {
        // Nothing to do.
    }

    public static String fieldTypeToDomain(FieldType fieldType) {
        switch (fieldType) {
            case STRING: return N2oDomain.STRING;
            case INTEGER: return N2oDomain.INTEGER;
            case FLOAT: return N2oDomain.FLOAT;
            case DATE: return N2oDomain.LOCALDATE;
            case BOOLEAN: return N2oDomain.BOOLEAN;
            default:
                throw new IllegalArgumentException("field type not supported");
        }
    }
}
