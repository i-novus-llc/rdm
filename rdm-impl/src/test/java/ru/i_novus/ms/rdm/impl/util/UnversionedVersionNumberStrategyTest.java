package ru.i_novus.ms.rdm.impl.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedVersionNumberStrategyTest {

    private static final String USED_VERSION = "-1.0";

    @InjectMocks
    private UnversionedVersionNumberStrategy strategy;

    @Test
    public void testFirst() {

        assertEquals(USED_VERSION, strategy.first());
    }

    @Test
    public void testNext() {

        assertEquals(USED_VERSION, strategy.next(null));
    }

    @Test
    public void testCheck() {

        assertTrue(strategy.check(USED_VERSION, null));
        assertFalse(strategy.check(null, null));
        assertFalse(strategy.check("", null));
        assertFalse(strategy.check("1.0", null));
    }
}