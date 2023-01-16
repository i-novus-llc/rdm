package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.field.ReferenceFilterValue;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.i_novus.ms.rdm.api.util.FieldValueUtils.*;
import static ru.i_novus.ms.rdm.api.util.StructureTestConstants.*;
import static ru.i_novus.ms.rdm.api.util.TimeUtils.DATE_PATTERN_ERA_FORMATTER;

@SuppressWarnings({"rawtypes", "java:S3740"})
public class FieldValueUtilsTest {

    private static final BigInteger ID_VALUE = BigInteger.ONE;
    private static final String CODE_VALUE = "code_value";
    private static final String NAME_VALUE = "some_name";
    private static final String STRING_VALUE = "string value";
    private static final BigInteger INTEGER_VALUE = BigInteger.valueOf(22L);
    private static final BigDecimal FLOAT_VALUE = BigDecimal.valueOf(33.33);
    private static final Boolean BOOLEAN_VALUE = Boolean.TRUE;
    private static final LocalDate DATE_VALUE = LocalDate.of(2021, 2, 3);
    //private static final Reference REFER_VALUE = new Reference("4", "four");

    @Test
    public void testToDisplayValueByRowValue() {

        DisplayExpression expression = DisplayExpression.ofField(CODE_ATTRIBUTE_CODE);
        List<FieldValue> fieldValues = createFieldValues();

        String result = toDisplayValue(expression.getValue(), new LongRowValue(1L, fieldValues), null);
        assertEquals(CODE_VALUE, result);
    }

    @Test
    public void testToDisplayValueWithoutPrimaries() {

        List<Map.Entry<String, String>> fields = createFields();
        DisplayExpression expression = DisplayExpression.ofFields(fields);
        List<FieldValue> fieldValues = createFieldValues();

        String expected = toFieldsDisplayValue(fields, fieldValues);
        String actual = toDisplayValue(expression.getValue(), fieldValues, null);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDisplayValueWithOnePrimary() {

        List<Map.Entry<String, String>> fields = createFields();
        DisplayExpression expression = DisplayExpression.ofFields(fields);
        List<FieldValue> fieldValues = createFieldValues();

        // CODE_ATTRIBUTE_CODE есть в expression:
        String expected = toFieldsDisplayValue(fields, fieldValues);
        String actual = toDisplayValue(expression.getValue(), fieldValues, singletonList(CODE_ATTRIBUTE_CODE));
        assertEquals(expected, actual);

        // ID_ATTRIBUTE_CODE нет в expression:
        expected = ID_VALUE + PRIMARY_KEY_VALUE_DISPLAY_DELIMITER + expected;
        actual = toDisplayValue(expression.getValue(), fieldValues, singletonList(ID_ATTRIBUTE_CODE));
        assertEquals(expected, actual);
    }

    @Test
    public void testToDisplayValueWhenAnyPresents() {

        List<Map.Entry<String, String>> fields = createFields();
        DisplayExpression expression = DisplayExpression.ofFields(fields);
        List<FieldValue> fieldValues = createFieldValues();
        List<String> primaryCodes = List.of(ID_ATTRIBUTE_CODE, CODE_ATTRIBUTE_CODE);

        // ID_ATTRIBUTE_CODE нет в expression, CODE_ATTRIBUTE_CODE есть в expression:
        String expected = toFieldsDisplayValue(fields, fieldValues);
        String actual = toDisplayValue(expression.getValue(), fieldValues, primaryCodes);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDisplayValueWhenNonePresents() {

        List<Map.Entry<String, String>> fields = createFields();
        fields = fields.subList(1, fields.size()); // Удаление CODE_ATTRIBUTE_CODE
        DisplayExpression expression = DisplayExpression.ofFields(fields);
        List<FieldValue> fieldValues = createFieldValues();
        List<String> primaryCodes = List.of(ID_ATTRIBUTE_CODE, CODE_ATTRIBUTE_CODE);

        // ID_ATTRIBUTE_CODE нет в expression, CODE_ATTRIBUTE_CODE нет в expression:
        String expected = ID_VALUE + PRIMARY_KEY_VALUE_DISPLAY_DELIMITER +
                CODE_VALUE + PRIMARY_KEY_VALUE_DISPLAY_DELIMITER +
                toFieldsDisplayValue(fields, fieldValues);
        String actual = toDisplayValue(expression.getValue(), fieldValues, primaryCodes);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDisplayValueWhenEmpty() {

        assertEquals("", toDisplayValue(null, (List) null, null));
        assertEquals("", toDisplayValue("", emptyList(), emptyList()));
    }

    private static List<Map.Entry<String, String>> createFields() {

        return List.of(
                new AbstractMap.SimpleEntry<>(CODE_ATTRIBUTE_CODE, ""),
                new AbstractMap.SimpleEntry<>(NAME_ATTRIBUTE_CODE, ""),

                new AbstractMap.SimpleEntry<>(STRING_ATTRIBUTE_CODE, ""),
                new AbstractMap.SimpleEntry<>(INTEGER_ATTRIBUTE_CODE, ""),
                new AbstractMap.SimpleEntry<>(FLOAT_ATTRIBUTE_CODE, ""),
                new AbstractMap.SimpleEntry<>(BOOLEAN_ATTRIBUTE_CODE, ""),
                new AbstractMap.SimpleEntry<>(DATE_ATTRIBUTE_CODE, "")//,
                //new AbstractMap.SimpleEntry<>(REFERENCE_ATTRIBUTE_CODE, "") // Исключено!
        );
    }

    private static List<FieldValue> createFieldValues() {

        return List.of(
                new IntegerFieldValue(ID_ATTRIBUTE_CODE, ID_VALUE),
                new StringFieldValue(CODE_ATTRIBUTE_CODE, CODE_VALUE),
                new StringFieldValue(NAME_ATTRIBUTE_CODE, NAME_VALUE),

                new StringFieldValue(STRING_ATTRIBUTE_CODE, STRING_VALUE),
                new IntegerFieldValue(INTEGER_ATTRIBUTE_CODE, INTEGER_VALUE),
                new FloatFieldValue(FLOAT_ATTRIBUTE_CODE, FLOAT_VALUE),
                new BooleanFieldValue(BOOLEAN_ATTRIBUTE_CODE, BOOLEAN_VALUE),
                new DateFieldValue(DATE_ATTRIBUTE_CODE, DATE_VALUE)//,
                //new ReferenceFieldValue(REFERENCE_ATTRIBUTE_CODE, REFER_VALUE) // Исключено!
        );
    }

    private static String toFieldsDisplayValue(List<Map.Entry<String, String>> fields, List<FieldValue> fieldValues) {

        return fieldValues.stream()
                .filter(fieldValue ->
                        fields.stream().anyMatch(entry -> Objects.equals(entry.getKey(), fieldValue.getField()))
                )
                .map(fieldValue -> fieldValue.getValue().toString())
                .collect(joining(" "));
    }

    @Test
    public void testCastFieldValueWhenReferenceFieldValue() {

        ReferenceFieldValue fieldValue = new ReferenceFieldValue("ref",
                new Reference(STRING_VALUE, STRING_VALUE + " displayed"));
        assertEquals(STRING_VALUE, castFieldValue(fieldValue, FieldType.STRING));
        assertNull(castFieldValue(fieldValue, null));

        ReferenceFieldValue nullValue = new ReferenceFieldValue("ref", new Reference(null, null));
        assertNull(castFieldValue(nullValue, FieldType.STRING));
        assertNull(castFieldValue(nullValue, null));
    }

    @Test
    public void testCastFieldValueWhenSimpleFieldValue() {

        BigInteger value = BigInteger.valueOf(11L);
        IntegerFieldValue fieldValue = new IntegerFieldValue("ref", value);
        assertEquals(value, castFieldValue(fieldValue, FieldType.INTEGER));
        assertEquals(value, castFieldValue(fieldValue, FieldType.STRING));
        assertEquals(value, castFieldValue(fieldValue, null));
    }

    @Test
    public void testCastReferenceValue() {

        assertEquals(STRING_VALUE, castReferenceValue(STRING_VALUE, FieldType.STRING));
        assertEquals(BOOLEAN_VALUE, castReferenceValue(BOOLEAN_VALUE.toString(), FieldType.BOOLEAN));
        assertEquals(DATE_VALUE, castReferenceValue(DATE_VALUE.format(DATE_PATTERN_ERA_FORMATTER), FieldType.DATE));
        assertEquals(FLOAT_VALUE.floatValue(), castReferenceValue(FLOAT_VALUE.toString(), FieldType.FLOAT));
        //assertEquals(FLOAT_VALUE, castReferenceValue(FLOAT_VALUE.toString(), FieldType.FLOAT));
        assertEquals(INTEGER_VALUE, castReferenceValue(INTEGER_VALUE.toString(), FieldType.INTEGER));

        String value = "unknown";
        assertEquals(value, castReferenceValue(value, FieldType.REFERENCE));
        assertEquals(value, castReferenceValue(value, FieldType.TREE));
    }

    @Test
    public void testToAttributeFilters() {

        ReferenceFilterValue stringValue = createReferenceFilterValue(STRING_ATTRIBUTE, STRING_VALUE);
        ReferenceFilterValue intValue = createReferenceFilterValue(INTEGER_ATTRIBUTE, INTEGER_VALUE.toString());
        List<ReferenceFilterValue> filterValues = List.of(stringValue, intValue);

        Set<List<AttributeFilter>> filterSet = toAttributeFilters(filterValues);
        assertEquals(2, filterSet.size());
    }

    @Test
    public void testToAttributeFiltersWhenEmpty() {

        Set<List<AttributeFilter>> filterSet = toAttributeFilters(emptyList());
        assertEquals(0, filterSet.size());
    }

    private ReferenceFilterValue createReferenceFilterValue(Structure.Attribute attribute, String value) {

        Reference reference = new Reference(value, value + " displayed");
        ReferenceFieldValue fieldValue = new ReferenceFieldValue(attribute.getCode(), reference);
        return new ReferenceFilterValue(attribute, fieldValue);
    }
}