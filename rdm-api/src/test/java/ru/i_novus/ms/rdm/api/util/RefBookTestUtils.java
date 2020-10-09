package ru.i_novus.ms.rdm.api.util;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.util.CollectionUtils.isEmpty;

@SuppressWarnings({"rawtypes", "java:S3740"})
public class RefBookTestUtils {

    private static final String WAITING_ERROR = "Ожидается ошибка ";

    private RefBookTestUtils() {
        // Nothing to do.
    }

    /**
     * Проверка объектов с учётом хеша и преобразования в строку.
     */
    public static void assertObjects(BiConsumer<Object, Object> objectAssert, Object current, Object actual) {

        objectAssert.accept(current, actual);

        if (current == null || actual == null)
            return;

        objectAssert.accept(current.hashCode(), actual.hashCode());

        if (isOverridingToString(current)) {
            objectAssert.accept(current.toString(), actual.toString());
        }
    }

    /** Проверка перекрытия `toString` в классе. */
    private static boolean isOverridingToString(Object o) {

        String actual = o.toString();
        if (StringUtils.isEmpty(actual))
            return true;

        String expected = o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
        return actual.equals(expected);
    }

    /**
     * Проверка списка на пустоту.
     */
    public static <T> void assertEmpty(List<T> list) {
        assertEquals(Collections.<T>emptyList(), list);
    }

    /**
     * Проверка набора на пустоту.
     */
    public static <K, V> void assertEmpty(Map<K, V> map) {
        assertEquals(Collections.<K, V>emptyMap(), map);
    }

    /**
     * Проверка объектов по особым условиям.
     */
    public static void assertSpecialEquals(Object current) {

        assertNotNull(current);
        assertObjects(Assert::assertEquals, current, current);
        assertObjects(Assert::assertNotEquals, current, null);

        Object other = (!BigInteger.ZERO.equals(current)) ? BigInteger.ZERO : BigInteger.ONE;
        assertObjects(Assert::assertNotEquals, current, other);
    }

    /**
     * Получение сообщения об ожидании исключения.
     */
    public static String getFailedMessage(Class expectedExceptionClass) {

        return WAITING_ERROR + expectedExceptionClass.getSimpleName();
    }

    /**
     * Получение кода сообщения об ошибке из исключения.
     */
    public static String getExceptionMessage(Exception e) {

        if (e instanceof UserException) {
            UserException ue = (UserException) e;

            if (!isEmpty(ue.getMessages()))
                return ue.getMessages().get(0).getCode();
        }

        if (!StringUtils.isEmpty(e.getMessage()))
            return e.getMessage();

        return null;
    }
}
