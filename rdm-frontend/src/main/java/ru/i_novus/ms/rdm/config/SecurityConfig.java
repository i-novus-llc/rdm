package ru.i_novus.ms.rdm.config;

import org.springframework.context.annotation.Configuration;

@Configuration
//@EnableWebSecurity
@SuppressWarnings("unused")
public class SecurityConfig {
//public class SecurityConfig extends OpenIdSecurityConfigurerAdapter {

    //@Value("${security.oauth2.client.client-id}")
    //private String clientId;
    //
    //@Value("${security.oauth2.resource.user-info-uri}")
    //private String userInfoUri;
    //
    //@Autowired
    //private AccountServiceRestClient accountServiceRestClient;
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
