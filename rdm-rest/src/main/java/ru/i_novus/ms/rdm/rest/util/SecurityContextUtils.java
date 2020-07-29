package ru.i_novus.ms.rdm.rest.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtils {

    private SecurityContextUtils() {}

    public static String getUserName() {
        String defaultName = "UNKNOWN";
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null)
            return defaultName;
        Authentication authentication = context.getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken)
            return defaultName;

        Object principal = authentication.getPrincipal();
        if (principal instanceof String)
            return (String) principal;
        return defaultName;
    }
}
