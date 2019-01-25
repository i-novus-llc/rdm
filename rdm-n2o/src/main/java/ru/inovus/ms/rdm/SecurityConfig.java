package ru.inovus.ms.rdm;

import net.n2oapp.framework.access.data.SecurityProvider;
import net.n2oapp.framework.access.simple.PermissionApi;
import net.n2oapp.framework.security.auth.oauth2.OpenIdSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2SsoProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import ru.inovus.ms.rdm.audit.AuditAuthenticationSuccessHandler;
import ru.inovus.ms.rdm.audit.AuditLogoutHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends OpenIdSecurityConfigurerAdapter {

    @Autowired
    AuditAuthenticationSuccessHandler auditAuthenticationSuccessHandler;

    @Autowired
    AuditLogoutHandler auditLogoutHandler;

    @Override
    protected void authorize(ExpressionUrlAuthorizationConfigurer<HttpSecurity>
                                     .ExpressionInterceptUrlRegistry url) throws Exception {
        //все запросы авторизованы
        url.anyRequest().authenticated()
                .and()
                .logout().addLogoutHandler(auditLogoutHandler)
                .and()
                .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
    }

    @Bean
    public SecurityProvider securityProvider(PermissionApi permissionApi) {
        return new SecurityProvider(permissionApi);
    }

    private OAuth2ClientAuthenticationProcessingFilter ssoFilter() {
        OAuth2SsoProperties ssoProps = this.getApplicationContext().getBean(OAuth2SsoProperties.class);

        OAuth2ClientAuthenticationProcessingFilter ssoFilter =
                new OAuth2ClientAuthenticationProcessingFilter(ssoProps.getLoginPath());
        ssoFilter.setAuthenticationSuccessHandler(auditAuthenticationSuccessHandler);
        ssoFilter.setRestTemplate(this.getApplicationContext()
                .getBean(UserInfoRestTemplateFactory.class).getUserInfoRestTemplate());
        ssoFilter.setTokenServices(this.getApplicationContext()
                .getBean(ResourceServerTokenServices.class));
        ssoFilter.setApplicationEventPublisher(this.getApplicationContext());
        return ssoFilter;
    }

}
