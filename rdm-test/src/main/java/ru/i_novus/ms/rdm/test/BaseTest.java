package ru.i_novus.ms.rdm.test;

import org.junit.Assert;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BaseTest {

    /** Check list for empty. */
    public <T> void assertEmpty(List<T> list) {
        assertEquals(Collections.<T>emptyList(), list);
    }

    /** Check map for empty. */
    public <K, V> void assertEmpty(Map<K, V> map) {
        assertEquals(Collections.<K, V>emptyMap(), map);
    }

    /** Check objects using `equals`, `hashCode`, and `toString`. */
    public <T> void assertObjects(BiConsumer<Object, Object> doAssert, T current, T actual) {

        doAssert.accept(current, actual);

        if (current == null || actual == null)
            return;

        if (isOverridingHashCode(current)) {
            doAssert.accept(current.hashCode(), actual.hashCode());
        }

        if (isOverridingToString(current)) {
            doAssert.accept(current.toString(), actual.toString());

        }
    }

    /** Check `hashCode` overriding in class. */
    private boolean isOverridingHashCode(Object o) {

        return o.hashCode() != System.identityHashCode(o);
    }

    /** Check `toString` overriding in class. */
    private boolean isOverridingToString(Object o) {

        String actual = o.toString();
        if (actual == null || actual.length() == 0)
            return true;

        String expected = o.getClass().getName() + "@" +
                Integer.toHexString(o.hashCode());

        return !actual.equals(expected);
    }

    /** Check objects by special branches of `equals`. */
    public void assertSpecialEquals(Object current) {

        assertNotNull(current);
        assertObjects(Assert::assertEquals, current, current);
        assertObjects(Assert::assertNotEquals, current, null);

        Object other = (!BigInteger.ZERO.equals(current)) ? BigInteger.ZERO : BigInteger.ONE;
        assertObjects(Assert::assertNotEquals, current, other);
    }

    /** Get expected class message to use in `fail`. */
    public String getFailedMessage(Class<?> expected) {
        return expected.getSimpleName() + " error expected";
    }

    /** Get exception message to use in `assert`. */
    public String getExceptionMessage(Exception exception) {
        return (exception != null) ? exception.getMessage() : null;
    }
}
