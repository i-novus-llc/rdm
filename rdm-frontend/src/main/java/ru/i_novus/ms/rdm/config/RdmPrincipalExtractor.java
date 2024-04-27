package ru.i_novus.ms.rdm.config;

import org.springframework.beans.factory.annotation.Value;

/**
 * Создание объекта пользователя из информации в SSO сервере
 */
//@Component
public class RdmPrincipalExtractor {
//public class RdmPrincipalExtractor extends GatewayPrincipalExtractor {

    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String SURNAME = "surname";
    private static final String PATRONYMIC = "patronymic";

    @Value("${rdm.sso.token.username:username}")
    private String username;

    //@Override
    //public Object extractPrincipal(Map<String, Object> map) {
    //    return new User((String) map.get(username), "N/A", extractAuthorities(map), (String) map.get(SURNAME),
    //            (String) map.get(NAME), (String) map.get(PATRONYMIC), (String) map.get(EMAIL));
    //}
}