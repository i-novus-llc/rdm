package ru.i_novus.ms.rdm.n2o.utils;

import java.lang.reflect.Field;

public class ReflectionUtils {

    private static final String ILLEGAL_ACCESS_ERROR =
            "Access not authorized on field '%s' of object '%s' with value: '%s'";

    private static final String ILLEGAL_ARGUMENT_ERROR =
            "Wrong argument on field '%s' of object '%s' with value: '%s',\n reason: %s";

    private ReflectionUtils() {
        throw new UnsupportedOperationException();
    }

    public static void setField(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);

        } catch (IllegalAccessException e) {

            final String errorMessage = String.format(ILLEGAL_ACCESS_ERROR, field, target, value);
            throw new RuntimeException(errorMessage, e);

        } catch (IllegalArgumentException e) {

            final String errorMessage = String.format(ILLEGAL_ARGUMENT_ERROR, field, target, value, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }
}
