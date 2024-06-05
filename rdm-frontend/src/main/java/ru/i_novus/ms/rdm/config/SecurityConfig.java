package ru.i_novus.ms.rdm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:/rdm-web.properties")
@SuppressWarnings("unused")
public class SecurityConfig {
}
