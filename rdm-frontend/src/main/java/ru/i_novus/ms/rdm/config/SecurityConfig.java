package ru.i_novus.ms.rdm.config;

import net.n2oapp.security.admin.rest.client.AccountServiceRestClient;
import net.n2oapp.security.admin.rest.client.AdminRestClientConfiguration;
import net.n2oapp.security.auth.OpenIdSecurityCustomizer;
import net.n2oapp.security.auth.context.account.ContextFilter;
import net.n2oapp.security.auth.context.account.ContextUserInfoTokenServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@Import({AdminRestClientConfiguration.class})
@AutoConfigureBefore(name = "net.n2oapp.framework.boot.N2oFrameworkAutoConfiguration")
@PropertySource("classpath:/rdm-web.properties")
@SuppressWarnings("unused")
public class SecurityConfig extends OpenIdSecurityCustomizer {

    private final AccountServiceRestClient accountServiceRestClient;

    private final OAuth2UserService<OidcUserRequest, OidcUser> userService;

    private final ContextUserInfoTokenServices tokenServices;

    @Autowired
    public SecurityConfig(
            AccountServiceRestClient accountServiceRestClient,
            OAuth2UserService<OidcUserRequest, OidcUser> userService,
            ContextUserInfoTokenServices tokenServices
    ) {
        this.accountServiceRestClient = accountServiceRestClient;
        this.userService = userService;
        this.tokenServices = tokenServices;
    }

    @Bean
    public RestTemplate rdmRestTemplate() {
        return new RestTemplate(); // for RdmWebConfiguration
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Override
    protected void configureHttpSecurity(HttpSecurity http) throws Exception {

        super.configureHttpSecurity(http);

        http.authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .oauth2Login()
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(userService));

        http.addFilterAfter(
                new ContextFilter(tokenServices, accountServiceRestClient),
                FilterSecurityInterceptor.class
        );
    }

    //@Value("${security.oauth2.client.client-id}")
    //private String clientId;
    //
    //@Value("${security.oauth2.resource.user-info-uri}")
    //private String userInfoUri;
    //
    //@Bean
    //@Primary
    //public RdmPrincipalExtractor gatewayPrincipalExtractor() {
    //    return new RdmPrincipalExtractor();
    //}
    //
    //@Override
    //protected void authorize(ExpressionUrlAuthorizationConfigurer<HttpSecurity>
    //                                 .ExpressionInterceptUrlRegistry url) throws Exception {
    //    // Все запросы авторизованы.
    //    url.anyRequest().authenticated()
    //            .and().logout()
    //            .and().addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
    //}
    //
    //private OAuth2ClientAuthenticationProcessingFilter ssoFilter() {
    //
    //    OAuth2SsoProperties ssoProps = this.getApplicationContext().getBean(OAuth2SsoProperties.class);
    //
    //    OAuth2ClientAuthenticationProcessingFilter ssoFilter =
    //            new OAuth2ClientAuthenticationProcessingFilter(ssoProps.getLoginPath());
    //
    //    SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
    //    successHandler.setAlwaysUseDefaultTargetUrl(true);
    //    ssoFilter.setAuthenticationSuccessHandler(successHandler);
    //
    //    ssoFilter.setRestTemplate(this.getApplicationContext()
    //            .getBean(UserInfoRestTemplateFactory.class).getUserInfoRestTemplate());
    //    ssoFilter.setTokenServices(this.getApplicationContext()
    //            .getBean(ResourceServerTokenServices.class));
    //    ssoFilter.setApplicationEventPublisher(this.getApplicationContext());
    //
    //    return ssoFilter;
    //}
    //
    //@Override
    //protected void configure(HttpSecurity http) throws Exception {
    //    super.configure(http);
    //    ContextUserInfoTokenServices tokenServices = new ContextUserInfoTokenServices(userInfoUri, clientId);
    //    http.addFilterAfter(new ContextFilter(tokenServices, accountServiceRestClient), FilterSecurityInterceptor.class);
    //}
}
