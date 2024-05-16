package ru.i_novus.ms.rdm.config;

import net.n2oapp.security.auth.OpenIdSecurityCustomizer;
import net.n2oapp.security.auth.common.UserAttributeKeys;
import net.n2oapp.security.auth.context.account.ContextUserInfoTokenServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Configuration
@ConditionalOnClass(OpenIdSecurityCustomizer.class)
public class SecurityContextConfig {

    @Value("${security.oauth2.resource.user-info-uri}")
    private String userInfoUri;

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> userService(UserAttributeKeys userAttributeKeys) {
        return new RdmUserService(userAttributeKeys);
    }

    @Bean
    public ContextUserInfoTokenServices tokenServices() {
        return new ContextUserInfoTokenServices(userInfoUri);
    }
}
