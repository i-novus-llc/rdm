package ru.i_novus.ms.rdm.rest.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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

    public static Object getPrincipal() {

        final Authentication authentication = getAuthentication();
        if (authentication == null)
            return null;

        return authentication.getPrincipal();
    }

    private static Authentication getAuthentication() {

        final SecurityContext context = SecurityContextHolder.getContext();
        return (context == null) ? null : context.getAuthentication();
    }
}
