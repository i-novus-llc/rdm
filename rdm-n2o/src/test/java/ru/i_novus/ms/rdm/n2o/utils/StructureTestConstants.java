package ru.i_novus.ms.rdm.n2o.utils;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.List;

public class StructureTestConstants {

    public static final String ID_ATTRIBUTE_CODE = "ID";
    public static final String NAME_ATTRIBUTE_CODE = "NAME";
    public static final String STRING_ATTRIBUTE_CODE = "CHAR";
    public static final String NUMBER_ATTRIBUTE_CODE = "NUMB";
    public static final String BOOLEAN_ATTRIBUTE_CODE = "BOOL";
    public static final String DATE_ATTRIBUTE_CODE = "DATE";

    public static final List<String> ATTRIBUTE_CODES = List.of(
            ID_ATTRIBUTE_CODE, NAME_ATTRIBUTE_CODE,
            STRING_ATTRIBUTE_CODE, NUMBER_ATTRIBUTE_CODE, BOOLEAN_ATTRIBUTE_CODE, DATE_ATTRIBUTE_CODE
    );

    public static final String REFERENCE_ATTRIBUTE_CODE = "REFER";
    public static final String SELF_REFER_ATTRIBUTE_CODE = "SELFY";

    public static final List<String> REFERENCE_CODES = List.of(
            REFERENCE_ATTRIBUTE_CODE, SELF_REFER_ATTRIBUTE_CODE
    );

    public static final String REFERRED_BOOK_CODE = "REFERRED";
    public static final String REFERRED_BOOK_ATTRIBUTE_CODE = "VALUE";
    public static final String SELF_REFERRED_BOOK_CODE = "SELFBOOK";
    public static final String SELF_REFERRED_BOOK_ATTRIBUTE_CODE = ID_ATTRIBUTE_CODE;
    public static final List<String> REFERRED_BOOK_CODES = List.of(
            REFERRED_BOOK_CODE, SELF_REFERRED_BOOK_CODE
    );

    public static final Structure.Attribute ID_ATTRIBUTE = Structure.Attribute.buildPrimary(
            ID_ATTRIBUTE_CODE, ID_ATTRIBUTE_CODE.toLowerCase(), FieldType.INTEGER, "primary");
    public static final Structure.Attribute NAME_ATTRIBUTE = Structure.Attribute.buildLocalizable(
            NAME_ATTRIBUTE_CODE, NAME_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "name");
    public static final Structure.Attribute STRING_ATTRIBUTE = Structure.Attribute.build(
            STRING_ATTRIBUTE_CODE, STRING_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "string");
    public static final Structure.Attribute NUMBER_ATTRIBUTE = Structure.Attribute.build(
            NUMBER_ATTRIBUTE_CODE, NUMBER_ATTRIBUTE_CODE.toLowerCase(), FieldType.INTEGER, "number");
    public static final Structure.Attribute BOOLEAN_ATTRIBUTE = Structure.Attribute.build(
            BOOLEAN_ATTRIBUTE_CODE, BOOLEAN_ATTRIBUTE_CODE.toLowerCase(), FieldType.BOOLEAN, "boolean");
    public static final Structure.Attribute DATE_ATTRIBUTE = Structure.Attribute.build(
            DATE_ATTRIBUTE_CODE, DATE_ATTRIBUTE_CODE.toLowerCase(), FieldType.DATE, "date");
    public static final Structure.Attribute REFERENCE_ATTRIBUTE = Structure.Attribute.build(
            REFERENCE_ATTRIBUTE_CODE, REFERENCE_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "reference");
    public static final Structure.Attribute SELF_REFER_ATTRIBUTE = Structure.Attribute.build(
            SELF_REFER_ATTRIBUTE_CODE, SELF_REFER_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "self-ref");

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
            STRING_ATTRIBUTE, NUMBER_ATTRIBUTE, BOOLEAN_ATTRIBUTE, DATE_ATTRIBUTE,
            REFERENCE_ATTRIBUTE, SELF_REFER_ATTRIBUTE
    );
    public static final List<Structure.Reference> REFERENCE_LIST = List.of(
            REFERENCE, SELF_REFER
    );

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

    private StructureTestConstants() {
        // Nothing to do.
    }
}
