package ru.i_novus.ms.rdm.impl;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

@SuppressWarnings({"java:S2187","java:S5778"})
public class BaseTest {

    private static final String ERROR_EXPECTED = " error expected";

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

    /** Check lists for equality. */
    public <T> void assertListEquals(List<T> expected, List<T> actual) {

        if (expected == null || expected.isEmpty()) {
            assertEmpty(actual);
            return;
        }

        assertEquals(expected.size(), actual.size());

        expected.forEach(item -> assertItemEquals(item, expected, actual));
    }

    /** Check lists for equality by item. */
    public <T> void assertItemEquals(T item, List<T> expected, List<T> actual) {

        if (item == null) {
            assertEquals(expected.stream().filter(Objects::isNull).count(),
                    actual.stream().filter(Objects::isNull).count()
            );
            return;
        }

        assertEquals(expected.stream().filter(item::equals).count(),
                actual.stream().filter(item::equals).count()
        );
    }

    /** Check lists for equality. */
    public <K, V> void assertMapEquals(Map<K, V> expected, Map<K, V> actual) {

        if (expected == null || expected.isEmpty()) {
            assertEmpty(actual);
            return;
        }

        assertEquals(expected.size(), actual.size());
        expected.forEach((k, v) -> assertItemEquals(k, v, actual));
    }

    /** Check maps for equality by item. */
    public <K, V> void assertItemEquals(K key, V value, Map<K, V> map) {

        assertTrue(map.containsKey(key));
        assertEquals(value, map.get(key));
    }

    /** Check objects by special branches of `equals`. */
    public void assertSpecialEquals(Object current) {

        assertNotNull(current);
        assertObjects(Assert::assertEquals, current, current);
        assertObjects(Assert::assertNotEquals, current, null);

        Object other = (!BigInteger.ZERO.equals(current)) ? BigInteger.ZERO : BigInteger.ONE;
        assertObjects(Assert::assertNotEquals, current, other);
    }

    /** Check method execution for success. */
    public void validateSuccess(MethodExecutor executor) {
        try {
            executor.execute();

        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    /** Check method execution for failure with single message. */
    public void validateFailure(MethodExecutor executor, Class<?> expectedClass, String expectedMessage) {
        try {
            executor.execute();
            fail(getFailedMessage(expectedClass));

        } catch (RuntimeException e) {
            assertEquals(expectedClass, e.getClass());
            assertEquals(expectedMessage, getExceptionMessage(e));
        }
    }

    /** Check method execution for failure with multiple messages. */
    public void validateFailure(MethodExecutor executor,
                                Class<? extends UserException> expectedClass,
                                List<String> expectedMessages) {
        try {
            executor.execute();
            fail(getFailedMessage(expectedClass));

        } catch (UserException e) {
            assertEquals(expectedClass, e.getClass());
            assertNotNull(e.getMessages());
            assertEquals(expectedMessages.size(), e.getMessages().size());

            for (int i = 0; i < expectedMessages.size(); i++) {
                assertEquals(expectedMessages.get(i), e.getMessages().get(i).getCode());
            }
        }
    }

    public interface MethodExecutor {
        void execute();
    }

    /** Get expected class message to use in `fail`. */
    public String getFailedMessage(Class<?> expected) {
        return expected.getSimpleName() + ERROR_EXPECTED;
    }

    /** Get exception message to use in `assert`. */
    public String getExceptionMessage(Exception exception) {
        return (exception != null) ? exception.getMessage() : null;
    }
}
