package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.util.StringUtils.*;

public class StringUtilsTest {

    @Test
    public void testIsEmpty() {

        assertTrue(isEmpty(null));
        assertTrue(isEmpty(""));

        assertFalse(isEmpty(" "));
        assertFalse(isEmpty("abc"));
    }

    @Test
    public void testAddDoubleQuotes() {

        assertEquals("\"\"", addDoubleQuotes(""));
        assertEquals("\"abc\"", addDoubleQuotes("abc"));
        assertEquals("\"a\"c\"", addDoubleQuotes("a\"c"));
    }

    @Test
    public void testAddSingleQuotes() {

        assertEquals("''", addSingleQuotes(""));
        assertEquals("'abc'", addSingleQuotes("abc"));
        assertEquals("'a'c'", addSingleQuotes("a'c"));
    }

    @Test
    public void testToDoubleQuotes() {

        assertEquals("\"\"", toDoubleQuotes(""));
        assertEquals("\"abc\"", toDoubleQuotes("abc"));
        assertEquals("\"a\\\"c\"", toDoubleQuotes("a\"c"));
    }

    @Test
    public void testToUuid() {

        assertNull(toUuid(null));
        assertNull(toUuid(""));
        assertNull(toUuid("  "));

        assertNotEquals(NULL_UUID, toUuid("12345678-9abc-1234-89ab-def012345678"));
        assertEquals(NULL_UUID, toUuid("Q2345678-9abc-1234-89ab-def012345678"));
    }

    @Test
    public void testJoinNumerated() {

        assertEquals("", joinNumerated(emptyList(), null, ","));
        assertEquals("1-a", joinNumerated(singletonList("a"), "%1$s-%2$s", ","));
        assertEquals("1-a,2-b", joinNumerated(List.of("a", "b"), "%1$s-%2$s", ","));
    }

    @Test
    public void testJoinNumeratedByDefault() {

        assertEquals("1) a\n2) b", joinNumerated(List.of("a", "b")));
    }
}