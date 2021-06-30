package ru.i_novus.ms.rdm.impl.util;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public class StructureTestConstants {

    public static final String ID_ATTRIBUTE_CODE = "ID";
    public static final String NAME_ATTRIBUTE_CODE = "NAME";
    public static final String STRING_ATTRIBUTE_CODE = "CHAR";
    public static final String INTEGER_ATTRIBUTE_CODE = "INT";
    public static final String FLOAT_ATTRIBUTE_CODE = "REAL";
    public static final String BOOLEAN_ATTRIBUTE_CODE = "BOOL";
    public static final String DATE_ATTRIBUTE_CODE = "DATE";

    public static final String CODE_ATTRIBUTE_CODE = "CODE";
    public static final String UNKNOWN_ATTRIBUTE_CODE = "Unknown";

    public static final List<String> PRIMARY_CODES = singletonList(ID_ATTRIBUTE_CODE);

    public static final List<String> ATTRIBUTE_CODES = List.of(
            ID_ATTRIBUTE_CODE, NAME_ATTRIBUTE_CODE,
            STRING_ATTRIBUTE_CODE,
            INTEGER_ATTRIBUTE_CODE, FLOAT_ATTRIBUTE_CODE,
            BOOLEAN_ATTRIBUTE_CODE, DATE_ATTRIBUTE_CODE
    );

    public static final String REFERENCE_ATTRIBUTE_CODE = "REFER";
    public static final String SELF_REFER_ATTRIBUTE_CODE = "SELFY";

    public static final List<String> REFERENCE_CODES = List.of(
            REFERENCE_ATTRIBUTE_CODE, SELF_REFER_ATTRIBUTE_CODE
    );

    public static final String REFERRED_BOOK_CODE = "REFERRED";
    public static final String REFERRED_BOOK_ATTRIBUTE_CODE = "VALUE";
    public static final Structure.Attribute REFERRED_ATTRIBUTE = Structure.Attribute.build(
            REFERRED_BOOK_ATTRIBUTE_CODE, REFERRED_BOOK_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "referred");

    public static final String SELF_REFERRED_BOOK_CODE = "SELFBOOK";
    public static final String SELF_REFERRED_BOOK_ATTRIBUTE_CODE = ID_ATTRIBUTE_CODE;
    public static final Structure.Attribute SELF_REFERRED_ATTRIBUTE = Structure.Attribute.buildPrimary(
            SELF_REFERRED_BOOK_ATTRIBUTE_CODE, SELF_REFERRED_BOOK_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "self-referred");

    public static final List<String> REFERRED_BOOK_CODES = List.of(
            REFERRED_BOOK_CODE, SELF_REFERRED_BOOK_CODE
    );

    public static final Structure.Attribute ID_ATTRIBUTE = Structure.Attribute.buildPrimary(
            ID_ATTRIBUTE_CODE, ID_ATTRIBUTE_CODE.toLowerCase(), FieldType.INTEGER, "primary key");
    public static final Structure.Attribute NAME_ATTRIBUTE = Structure.Attribute.buildLocalizable(
            NAME_ATTRIBUTE_CODE, NAME_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, null); // без описания!
    public static final Structure.Attribute STRING_ATTRIBUTE = Structure.Attribute.build(
            STRING_ATTRIBUTE_CODE, STRING_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "string-typed");
    public static final Structure.Attribute INTEGER_ATTRIBUTE = Structure.Attribute.build(
            INTEGER_ATTRIBUTE_CODE, INTEGER_ATTRIBUTE_CODE.toLowerCase(), FieldType.INTEGER, "integer-typed");
    public static final Structure.Attribute FLOAT_ATTRIBUTE = Structure.Attribute.build(
            FLOAT_ATTRIBUTE_CODE, FLOAT_ATTRIBUTE_CODE.toLowerCase(), FieldType.FLOAT, "float-typed");
    public static final Structure.Attribute BOOLEAN_ATTRIBUTE = Structure.Attribute.build(
            BOOLEAN_ATTRIBUTE_CODE, BOOLEAN_ATTRIBUTE_CODE.toLowerCase(), FieldType.BOOLEAN, "boolean-typed");
    public static final Structure.Attribute DATE_ATTRIBUTE = Structure.Attribute.build(
            DATE_ATTRIBUTE_CODE, DATE_ATTRIBUTE_CODE.toLowerCase(), FieldType.DATE, "date-typed");
    public static final Structure.Attribute REFERENCE_ATTRIBUTE = Structure.Attribute.build(
            REFERENCE_ATTRIBUTE_CODE, REFERENCE_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "reference");
    public static final Structure.Attribute SELF_REFER_ATTRIBUTE = Structure.Attribute.build(
            SELF_REFER_ATTRIBUTE_CODE, SELF_REFER_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "self-ref");

    public static final Structure.Attribute CODE_ATTRIBUTE = Structure.Attribute.buildPrimary(
            CODE_ATTRIBUTE_CODE, CODE_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "primary code");

    public static final Structure.Reference REFERENCE = new Structure.Reference(
            REFERENCE_ATTRIBUTE_CODE, REFERRED_BOOK_CODE,
            DisplayExpression.toPlaceholder(REFERRED_BOOK_ATTRIBUTE_CODE)
    );
    public static final Structure.Reference SELF_REFER = new Structure.Reference(
            SELF_REFER_ATTRIBUTE_CODE, SELF_REFERRED_BOOK_CODE,
            DisplayExpression.toPlaceholder(SELF_REFERRED_BOOK_ATTRIBUTE_CODE)
    );

    public static final List<Structure.Attribute> ATTRIBUTE_LIST = List.of(
            ID_ATTRIBUTE, NAME_ATTRIBUTE,
            STRING_ATTRIBUTE,
            INTEGER_ATTRIBUTE, FLOAT_ATTRIBUTE,
            BOOLEAN_ATTRIBUTE, DATE_ATTRIBUTE,
            REFERENCE_ATTRIBUTE, SELF_REFER_ATTRIBUTE
    );
    public static final List<Structure.Reference> REFERENCE_LIST = List.of(
            REFERENCE, SELF_REFER
    );

    public static final Structure DEFAULT_STRUCTURE = new Structure(ATTRIBUTE_LIST, REFERENCE_LIST);

    public static final String CHANGE_ATTRIBUTE_CODE = "CHANGE";
    public static final Structure.Attribute CHANGE_ATTRIBUTE = Structure.Attribute.build(
            CHANGE_ATTRIBUTE_CODE, CHANGE_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "change");

    public static final String CHANGE_REF_ATTRIBUTE_CODE = "CHANGE_REF";
    public static final Structure.Attribute CHANGE_REF_ATTRIBUTE = Structure.Attribute.build(
            CHANGE_REF_ATTRIBUTE_CODE, CHANGE_REF_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "change-ref");
    public static final Structure.Reference CHANGE_REF_REFERENCE = new Structure.Reference(
            CHANGE_REF_ATTRIBUTE_CODE, REFERRED_BOOK_CODE,
            DisplayExpression.toPlaceholder(REFERRED_BOOK_ATTRIBUTE_CODE)
    );

    public static final Structure REFERRED_STRUCTURE = new Structure(List.of(ID_ATTRIBUTE, REFERRED_ATTRIBUTE), null);
    public static final Structure SELF_REFERRED_STRUCTURE = new Structure(List.of(SELF_REFERRED_ATTRIBUTE), null);

    private StructureTestConstants() {
        // Nothing to do.
    }

    public static List<String> getAllAttributeCodes() {

        List<String> allAttributeCodes = new ArrayList<>();
        allAttributeCodes.addAll(ATTRIBUTE_CODES);
        allAttributeCodes.addAll(REFERENCE_CODES);

        return allAttributeCodes;
    }
}
