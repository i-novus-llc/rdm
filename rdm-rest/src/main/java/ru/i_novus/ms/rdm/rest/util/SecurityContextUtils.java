package ru.i_novus.ms.rdm.rest.util;

public final class SecurityContextUtils {

    private static final String DEFAULT_USER_ID = "UNKNOWN";
    private static final String DEFAULT_USER_NAME = "UNKNOWN";

    private SecurityContextUtils() {
        // Nothing to do.
    }

    public static String getUserId() {
        return DEFAULT_USER_ID;
    }

    public static String getUserName() {
        return DEFAULT_USER_NAME;
    }
}
