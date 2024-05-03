package ru.i_novus.ms.rdm.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeRequests()
                .antMatchers("/*").permitAll()
                //.anyRequest().authenticated()
                //.and()
                //.csrf().disable()
        ;
        //http.oauth2ResourceServer(configurer -> configurer.jwt());

        return http.build();
    }
}
