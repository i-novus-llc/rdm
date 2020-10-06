package ru.inovus.ms.rdm;

import net.n2oapp.framework.security.auth.oauth2.gateway.GatewayPrincipalExtractor;
import net.n2oapp.security.auth.common.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Создание объекта пользователя из информации в SSO сервере
 */
@Component
public class RdmPrincipalExtractor extends GatewayPrincipalExtractor {

    @Value("${rdm.sso.token.username:username}")
    private String username;
    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String SURNAME = "surname";
    private static final String PATRONYMIC = "patronymic";

    @Override
    public Object extractPrincipal(Map<String, Object> map) {
        return new User((String) map.get(username), "N/A", extractAuthorities(map), (String) map.get(SURNAME),
                (String) map.get(NAME), (String) map.get(PATRONYMIC), (String) map.get(EMAIL));
    }
}