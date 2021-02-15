package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.i_novus.ms.rdm.api.util.StringUtils.*;

public class StringUtilsTest {

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
    public void testToSingleQuotes() {

        assertEquals("''", toSingleQuotes(""));
        assertEquals("'abc'", toSingleQuotes("abc"));
        assertEquals("'a''c'", toSingleQuotes("a'c"));
    }

    @Test
    public void testToDoubleQuotes() {

        assertEquals("\"\"", toDoubleQuotes(""));
        assertEquals("\"abc\"", toDoubleQuotes("abc"));
        assertEquals("\"a\"\"c\"", toDoubleQuotes("a\"c"));
    }
}