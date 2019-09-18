package ru.inovus.ms.rdm.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.Structure;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PkUniqueRowAppendValidationTest {

    private final String PK_STRING = "pks";
    private final String PK_REFERENCE = "pkr";
    private final String PK_FLOAT = "pkf";
    private final String PK_DATE = "pkd";
    private final String PK_BOOL = "pkb";
    private final String PK_INTEGER = "pki";
    private final String NOT_PK = "npk";


    private Structure pkStructure;

    private Structure noPkStructure;

    private Row pkRow1;
    private Row pkRow2;

    private Row noPkRow;


    @Before
    public void setUp() {
        pkStructure = new Structure();
        pkStructure.setAttributes(Arrays.asList(
                Structure.Attribute.buildPrimary(PK_STRING, "string", FieldType.STRING, "string"),
                Structure.Attribute.buildPrimary(PK_REFERENCE, "reference", FieldType.REFERENCE, "count"),
                Structure.Attribute.buildPrimary(PK_FLOAT, "float", FieldType.FLOAT, "float"),
                Structure.Attribute.buildPrimary(PK_DATE, "date", FieldType.DATE, "date"),
                Structure.Attribute.buildPrimary(PK_BOOL, "boolean", FieldType.BOOLEAN, "boolean"),
                Structure.Attribute.buildPrimary(PK_INTEGER, "integer", FieldType.INTEGER, "integer"),
                Structure.Attribute.build(NOT_PK, "not_pk", FieldType.INTEGER, "integer")
        ));

        Map<String, Object> pkRowMap1 = new HashMap<>();
        pkRowMap1.put(PK_STRING, "test Value");
        pkRowMap1.put(PK_REFERENCE, new Reference());
        pkRowMap1.put(PK_FLOAT, "test Value");
        pkRowMap1.put(PK_DATE, "test Value");
        pkRowMap1.put(PK_BOOL, "test Value");
        pkRowMap1.put(PK_INTEGER, "test Value");
        pkRowMap1.put(NOT_PK, "test Value");
        pkRow1 = new Row(pkRowMap1);

        Map<String, Object> pkRowMap2 = new HashMap<>();
        pkRowMap2.put(PK_STRING, "test Value 1");
        pkRowMap2.put(PK_REFERENCE, new Reference());
        pkRowMap2.put(PK_FLOAT, "test Value");
        pkRowMap2.put(PK_DATE, "test Value");
        pkRowMap2.put(PK_BOOL, "test Value");
        pkRowMap2.put(PK_INTEGER, "test Value");
        pkRowMap2.put(NOT_PK, "test Value");
        pkRow2 = new Row(pkRowMap2);

        noPkStructure = new Structure();
        noPkStructure.setAttributes(Collections.singletonList(
                Structure.Attribute.build(NOT_PK, "not_pk", FieldType.INTEGER, "integer")
        ));

        Map<String, Object> noPkRowMap = new HashMap<>();
        noPkRowMap.put(NOT_PK, "test Value");
        noPkRow = new Row(noPkRowMap);

    }

    @Test
    public void testValidate(){

        PkUniqueRowAppendValidation pkUniqueRowAppendValidation = new PkUniqueRowAppendValidation(pkStructure);
        pkUniqueRowAppendValidation.appendRow(pkRow1);
        assertEquals(0, pkUniqueRowAppendValidation.validate().size());
        pkUniqueRowAppendValidation.appendRow(pkRow1);
        assertEquals(1, pkUniqueRowAppendValidation.validate().size());
        pkUniqueRowAppendValidation.appendRow(pkRow2);
        assertEquals(0, pkUniqueRowAppendValidation.validate().size());
        pkUniqueRowAppendValidation.appendRow(pkRow2);
        assertEquals(1, pkUniqueRowAppendValidation.validate().size());
        pkUniqueRowAppendValidation.appendRow(pkRow1);
        assertEquals(1, pkUniqueRowAppendValidation.validate().size());

    }

}