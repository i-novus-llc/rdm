package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.util.StructureTestConstants.*;
import static ru.i_novus.ms.rdm.api.util.StructureUtils.*;

public class StructureUtilsTest extends BaseTest {

    private static final Structure.Reference NULL_REFERENCE = new Structure.Reference();
    private static final Structure.Reference BAD_REFERENCE = new Structure.Reference("attr", null, null);

    private static final Structure.Reference PARTIAL_REFERENCE = new Structure.Reference("attr", "ref", null);
    private static final Structure.Reference FULL_REFERENCE = new Structure.Reference("attr", "ref", "${code}");

    @Test
    public void testIsReference() {

        assertFalse(isReference(null));

        assertFalse(isReference(NULL_REFERENCE));
        assertFalse(isReference(BAD_REFERENCE));

        assertTrue(isReference(PARTIAL_REFERENCE));
        assertTrue(isReference(FULL_REFERENCE));
    }

    @Test
    public void testIsDisplayExpressionEquals() {

        assertFalse(isDisplayExpressionEquals(null, null));

        assertIsDisplayExpressionEquals(NULL_REFERENCE);
        assertIsDisplayExpressionEquals(BAD_REFERENCE);

        assertIsDisplayExpressionEquals(PARTIAL_REFERENCE);
        assertIsDisplayExpressionEquals(FULL_REFERENCE);
    }

    private void assertIsDisplayExpressionEquals(Structure.Reference reference) {

        assertFalse(isDisplayExpressionEquals(null, reference));
        assertFalse(isDisplayExpressionEquals(reference, null));
        assertTrue(isDisplayExpressionEquals(reference, reference));

        String displayExpression = reference.getDisplayExpression();

        Structure.Reference other = new Structure.Reference(
                reference.getAttribute(),
                reference.getReferenceCode(),
                DisplayExpression.ofField(displayExpression == null ? "code" : "other_code").getValue()
        );
        assertFalse(isDisplayExpressionEquals(reference, other));
    }

    @Test
    public void testContainsAnyPlaceholder() {

        assertFalse(containsAnyPlaceholder(null, emptyList()));
        assertFalse(containsAnyPlaceholder(null, singletonList("name")));
        assertFalse(containsAnyPlaceholder("", emptyList()));
        assertFalse(containsAnyPlaceholder("", singletonList("name")));

        String displayExpression = DisplayExpression.ofFields("code", "value").getValue();
        assertFalse(containsAnyPlaceholder(displayExpression, emptyList()));
        assertFalse(containsAnyPlaceholder(displayExpression, singletonList("name")));

        assertTrue(containsAnyPlaceholder(displayExpression, singletonList("code")));
        assertTrue(containsAnyPlaceholder(displayExpression, List.of("code", "name")));
        assertTrue(containsAnyPlaceholder(displayExpression, List.of("code", "value")));
    }

    @Test
    public void testHasAbsentPlaceholder() {

        final Structure structure = DEFAULT_STRUCTURE;

        assertFalse(hasAbsentPlaceholder(null, structure));
        assertFalse(hasAbsentPlaceholder("", structure));

        assertFalse(hasAbsentPlaceholder(DisplayExpression.ofField(ID_ATTRIBUTE_CODE).getValue(), structure));
        assertTrue(hasAbsentPlaceholder(DisplayExpression.ofField(UNKNOWN_ATTRIBUTE_CODE).getValue(), structure));
    }

    @Test
    public void testGetAbsentPlaceholders() {

        final Structure structure = DEFAULT_STRUCTURE;

        assertListEquals(emptyList(), getAbsentPlaceholders(null, structure));
        assertListEquals(emptyList(), getAbsentPlaceholders("", structure));

        String absent1 = ID_ATTRIBUTE_CODE + "1";
        String absent2 = ID_ATTRIBUTE_CODE + "2";
        List<String> expected = List.of(absent1, absent2);

        DisplayExpression displayExpression = DisplayExpression.ofFields(ID_ATTRIBUTE_CODE, absent1, absent2);
        List<String> actual = getAbsentPlaceholders(displayExpression.getValue(), structure);
        assertListEquals(expected, actual);
    }

    @Test
    public void testDisplayExpressionToPlaceholder() {

        assertNull(displayExpressionToPlaceholder(null));
        assertNull(displayExpressionToPlaceholder(""));

        DisplayExpression displayExpression = DisplayExpression.ofFields(ID_ATTRIBUTE_CODE);
        assertEquals(ID_ATTRIBUTE_CODE, displayExpressionToPlaceholder(displayExpression.getValue()));

        displayExpression = DisplayExpression.ofFields(ID_ATTRIBUTE_CODE, NAME_ATTRIBUTE_CODE);
        assertNull(displayExpressionToPlaceholder(displayExpression.getValue()));
    }
}