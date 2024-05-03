package ru.i_novus.ms.rdm.rest.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtils {

    //private static final Logger logger = LoggerFactory.getLogger(SecurityContextUtils.class);

    private static final String DEFAULT_USER_ID = "UNKNOWN";
    private static final String DEFAULT_USER_NAME = "UNKNOWN";

    private SecurityContextUtils() {
        // Nothing to do.
    }

    public static String getUserId() {

        return DEFAULT_USER_ID;

        //final Jwt tokenValue = getTokenValue();
        //final Map<String, Object> claims = (tokenValue != null) ? tokenValue.getClaims() : null;
        //
        //final Object value = (claims != null) ? claims.get("email") : null;
        //return (value != null) ? (String) value : DEFAULT_USER_ID;
    }

    //private static Jwt getTokenValue() {
    //
    //    Authentication authentication = getAuthentication();
    //    if (authentication == null)
    //        return null;
    //
    //    Object details = authentication.getDetails();
    //    String tokenValue = (details instanceof OAuth2AuthenticationDetails)
    //            ? ((OAuth2AuthenticationDetails) details).getTokenValue()
    //            : null;
    //    return isEmpty(tokenValue) ? null : JwtHelper.decode(tokenValue);
    //}

    public static String getUserName() {

        final Object principal = getPrincipal();
        if (principal instanceof String)
            return (String) principal;

        return DEFAULT_USER_NAME;
    }

    private static Object getPrincipal() {

        final Authentication authentication = getAuthentication();
        if (authentication == null)
            return null;

        return authentication.getPrincipal();
    }

    private static Authentication getAuthentication() {

        final SecurityContext context = SecurityContextHolder.getContext();
        if (context == null)
            return null;

        final Authentication authentication = context.getAuthentication();
        return (authentication instanceof AnonymousAuthenticationToken) ? null : authentication;
    }
}
