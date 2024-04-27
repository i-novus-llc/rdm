package ru.i_novus.ms.rdm.config;

public final class SecurityContextUtils {

    public static final String DEFAULT_USER_ID = "UNKNOWN";
    public static final String DEFAULT_USER_NAME = "UNKNOWN";

    private SecurityContextUtils() {
        // Nothing to do.
    }

    //public static User getPrincipal() {
    //
    //    Authentication authentication = getAuthentication();
    //    if (authentication == null)
    //        return null;
    //
    //    return (User) authentication.getPrincipal();
    //}
    //
    //private static Authentication getAuthentication() {
    //
    //    SecurityContext context = SecurityContextHolder.getContext();
    //    if (context == null)
    //        return null;
    //
    //    Authentication authentication = context.getAuthentication();
    //    return (authentication instanceof AnonymousAuthenticationToken) ? null : authentication;
    //}
}
