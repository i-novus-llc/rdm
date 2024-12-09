package ru.i_novus.ms.rdm.rest.autoconfigure.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtils {

    public static final String DEFAULT_USER_ID = "UNKNOWN";
    public static final String DEFAULT_USER_NAME = "UNKNOWN";

    public SecurityContextUtils() {
        // Nothing to do.
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
