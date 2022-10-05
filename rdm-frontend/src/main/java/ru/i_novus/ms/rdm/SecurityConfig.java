package ru.i_novus.ms.rdm;

import net.n2oapp.security.admin.rest.client.AccountServiceRestClient;
import net.n2oapp.security.auth.OpenIdSecurityConfigurerAdapter;
import net.n2oapp.security.auth.common.context.ContextFilter;
import net.n2oapp.security.auth.common.context.ContextUserInfoTokenServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2SsoProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@SuppressWarnings("unused")
public class SecurityConfig extends OpenIdSecurityConfigurerAdapter {

    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.resource.user-info-uri}")
    private String userInfoUri;

    @Autowired
    private AccountServiceRestClient accountServiceRestClient;

    @Bean
    @Primary
    public RdmPrincipalExtractor gatewayPrincipalExtractor() {
        return new RdmPrincipalExtractor();
    }

    @Override
    protected void authorize(ExpressionUrlAuthorizationConfigurer<HttpSecurity>
                                     .ExpressionInterceptUrlRegistry url) throws Exception {
        // Все запросы авторизованы.
        url.anyRequest().authenticated()
                .and().logout()
                .and().addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
    }

    private OAuth2ClientAuthenticationProcessingFilter ssoFilter() {

        OAuth2SsoProperties ssoProps = this.getApplicationContext().getBean(OAuth2SsoProperties.class);

        OAuth2ClientAuthenticationProcessingFilter ssoFilter =
                new OAuth2ClientAuthenticationProcessingFilter(ssoProps.getLoginPath());

        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        ssoFilter.setAuthenticationSuccessHandler(successHandler);

        ssoFilter.setRestTemplate(this.getApplicationContext()
                .getBean(UserInfoRestTemplateFactory.class).getUserInfoRestTemplate());
        ssoFilter.setTokenServices(this.getApplicationContext()
                .getBean(ResourceServerTokenServices.class));
        ssoFilter.setApplicationEventPublisher(this.getApplicationContext());

        return ssoFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        ContextUserInfoTokenServices tokenServices = new ContextUserInfoTokenServices(userInfoUri, clientId);
        http.addFilterAfter(new ContextFilter(tokenServices, accountServiceRestClient), FilterSecurityInterceptor.class);
    }
}
