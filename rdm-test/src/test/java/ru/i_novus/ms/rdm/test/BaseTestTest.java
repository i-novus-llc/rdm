package ru.i_novus.ms.rdm.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigInteger;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseTestTest {

    @InjectMocks
    private BaseTest baseTest;

    @Test
    public void testAssertEmptyList() {

        baseTest.assertEmpty(emptyList());

        List<Integer> empty = new ArrayList<>();
        baseTest.assertEmpty(empty);
    }

    @Test
    public void testAssertEmptyListFailed() {

        testAssertEmptyListFailed(null);

        List<Integer> notEmpty = new ArrayList<>();
        notEmpty.add(1);

        testAssertEmptyListFailed(notEmpty);
    }

    private void testAssertEmptyListFailed(List<Integer> items) {

        try {
            baseTest.assertEmpty(items);
            fail();

        } catch (AssertionError e) {
            // Nothing to do.
        }
    }

    @Test
    public void testAssertEmptyMap() {

        baseTest.assertEmpty(emptyMap());

        Map<String, Integer> empty = new HashMap<>();
        baseTest.assertEmpty(empty);
    }

    @Test
    public void testAssertEmptyMapFailed() {

        testAssertEmptyMapFailed(null);

        Map<String, Integer> notEmpty = new HashMap<>();
        notEmpty.put("1", 1);

        testAssertEmptyMapFailed(notEmpty);
    }

    private void testAssertEmptyMapFailed(Map<String, Integer> items) {

        try {
            baseTest.assertEmpty(items);
            fail();

        } catch (AssertionError e) {
            // Nothing to do.
        }
    }

    @Test
    public void testAssertObjects() {

        baseTest.assertObjects(Assert::assertEquals, BigInteger.ONE, BigInteger.ONE);
        baseTest.assertObjects(Assert::assertNotEquals, BigInteger.ONE, BigInteger.TWO);
    }

    @Test
    public void testAssertListEquals() {

        baseTest.assertListEquals(emptyList(), emptyList());

        List<BigInteger> list = List.of(BigInteger.ONE, BigInteger.TWO);
        baseTest.assertListEquals(list, list);
    }

    @Test
    public void testAssertMapEquals() {

        baseTest.assertMapEquals(emptyMap(), emptyMap());

        Map<String, BigInteger> map = new HashMap<>(2);
        map.put("1", BigInteger.ONE);
        map.put("2", BigInteger.TWO);

        baseTest.assertMapEquals(map, map);
    }

    @Test
    public void testAssertSpecialEquals() {

        baseTest.assertSpecialEquals(BigInteger.ZERO);
        baseTest.assertSpecialEquals(BigInteger.ONE);
    }

    @Test
    public void testGetFailedMessage() {

        String message = baseTest.getFailedMessage(IllegalArgumentException.class);
        assertNotNull(message);
        assertTrue(message.contains(IllegalArgumentException.class.getSimpleName()));
    }

    @Test
    public void testGetExceptionMessage() {

        final String text = "illegal";
        String message = baseTest.getExceptionMessage(new IllegalArgumentException(text));
        assertNotNull(message);
        assertTrue(message.contains(text));
    }
}