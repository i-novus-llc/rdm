package ru.i_novus.ms.rdm;

import net.n2oapp.security.auth.common.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtils {

    public static final String DEFAULT_USER_ID = "UNKNOWN";
    public static final String DEFAULT_USER_NAME = "UNKNOWN";

    private SecurityContextUtils() {
        throw new UnsupportedOperationException();
    }

    public static User getPrincipal() {

        Authentication authentication = getAuthentication();
        if (authentication == null)
            return null;

        return (User) authentication.getPrincipal();
    }

    private static Authentication getAuthentication() {

        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null)
            return null;

        Authentication authentication = context.getAuthentication();
        return (authentication instanceof AnonymousAuthenticationToken) ? null : authentication;
    }
}
