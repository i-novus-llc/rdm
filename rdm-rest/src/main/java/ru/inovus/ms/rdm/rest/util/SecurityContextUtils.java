package ru.inovus.ms.rdm.rest.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SecurityContextUtils {

    private static String getAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("Request attributes is empty");
        }

        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            throw new IllegalStateException(HttpHeaders.AUTHORIZATION + " header doesn't exist");
        }

        return authorization;
    }

    /**
     * Получение значения из ноды jwt-токена
     */
    private static String getJwtNodeValue(String jwt, String jwtNodeName) {
        String[] jwtParts = jwt.split("\\.");
        String jwtBodyEncoded = jwtParts[1];

        String jwtBodyDecoded = new String(new Base64(true).decode(jwtBodyEncoded));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jwtBodyNode;
        try {
            jwtBodyNode = objectMapper.readTree(jwtBodyDecoded);
            return jwtBodyNode.get(jwtNodeName).asText();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed extracting node \'" + jwtNodeName + "\' in JWT token", e);
        }
    }

    private static String getJwtTokenValue(String jwtNodeName) {
        return getJwtNodeValue(getAuthorizationHeader(), jwtNodeName);
    }

    public static String getUserName() {
        return getJwtTokenValue("preferred_username");
    }
}
