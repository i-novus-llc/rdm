package ru.i_novus.ms.rdm.n2o.api.util;

import org.junit.Test;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.*;

public class DataRecordUtilsTest {

    private static final String FIELD_PREFIX = addPrefix("");

    private static final String FIELD_PART_SEPARATOR = addFieldPart("", "");

    private static final String FIELD_PROPERTY_SEPARATOR = addFieldProperty("", "");

    private static final String TEST_FIELD = "field";
    private static final String TEST_FIELD_PART = "part";
    private static final String TEST_FIELD_PROPERTY = "prop";
    private static final String PREFIXED_FIELD = FIELD_PREFIX + TEST_FIELD;

    @Test
    public void testAddPrefix() {

        assertNull(addPrefix(null));
        assertEquals(FIELD_PREFIX, addPrefix(""));

        assertEquals(PREFIXED_FIELD, addPrefix(TEST_FIELD));
    }

    @Test
    public void testDeletePrefix() {

        assertNull(deletePrefix(null));
        assertEquals("", deletePrefix(FIELD_PREFIX));

        assertEquals(TEST_FIELD, deletePrefix(PREFIXED_FIELD));
    }

    @Test
    public void testHasPrefix() {

        assertFalse(hasPrefix(null));
        assertFalse(hasPrefix(""));

        assertTrue(hasPrefix(FIELD_PREFIX));
        assertTrue(hasPrefix(PREFIXED_FIELD));

        assertFalse(hasPrefix(TEST_FIELD + FIELD_PREFIX));
    }

    @Test
    public void testAddFieldPart() {

        assertEquals(FIELD_PART_SEPARATOR, addFieldPart("", ""));
        assertEquals(TEST_FIELD + FIELD_PART_SEPARATOR + TEST_FIELD_PART,
                addFieldPart(TEST_FIELD, TEST_FIELD_PART));
    }

    @Test
    public void testAddFieldProperty() {

        assertEquals(FIELD_PROPERTY_SEPARATOR, addFieldProperty("", ""));
        assertEquals(TEST_FIELD + FIELD_PROPERTY_SEPARATOR + TEST_FIELD_PROPERTY,
                addFieldProperty(TEST_FIELD, TEST_FIELD_PROPERTY));
    }
}