package ru.i_novus.ms.rdm.rest.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.IOException;

import static org.springframework.util.StringUtils.isEmpty;

public class SecurityContextUtils {

    private static final Logger logger = LoggerFactory.getLogger(SecurityContextUtils.class);

    private static final String DEFAULT_USER_ID = "UNKNOWN";
    private static final String DEFAULT_USER_NAME = "UNKNOWN";

    private SecurityContextUtils() {
        throw new UnsupportedOperationException();
    }

    public static String getUserId() {

        Jwt tokenValue = getTokenValue();
        String claims = (tokenValue != null) ? tokenValue.getClaims() : null;

        JsonNode jsonNode = claimsToJson(claims);
        JsonNode jsonValue = (jsonNode != null) ? jsonNode.get("email") : null;

        return (jsonValue != null) ? jsonValue.asText() : DEFAULT_USER_ID;
    }

    private static Jwt getTokenValue() {

        Authentication authentication = getAuthentication();
        if (authentication == null)
            return null;

        Object details = authentication.getDetails();
        String tokenValue = (details instanceof OAuth2AuthenticationDetails)
                ? ((OAuth2AuthenticationDetails) details).getTokenValue()
                : null;
        return isEmpty(tokenValue) ? null : JwtHelper.decode(tokenValue);
    }

    private static JsonNode claimsToJson(String claims) {

        if (isEmpty(claims))
            return null;

        JsonNode jsonNode;
        try {
            jsonNode = JsonUtil.jsonMapper.readTree(claims);

        } catch (IOException e) {
            logger.error(String.format("Error reading token value claims from%n%s", claims), e);

            jsonNode = null;
        }
        return jsonNode;
    }

    public static String getUserName() {

        Object principal = getPrincipal();
        if (principal instanceof String)
            return (String) principal;

        return DEFAULT_USER_NAME;
    }

    private static Object getPrincipal() {

        Authentication authentication = getAuthentication();
        if (authentication == null)
            return null;

        return authentication.getPrincipal();
    }

    private static Authentication getAuthentication() {

        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null)
            return null;

        Authentication authentication = context.getAuthentication();
        return (authentication instanceof AnonymousAuthenticationToken) ? null : authentication;
    }
}
