package ru.i_novus.ms.rdm.n2o.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Configuration
@Import(value = { ClientConfiguration.class, N2oConfiguration.class })
@ComponentScan(basePackages = "ru.i_novus.ms.rdm.n2o")
public class RdmWebConfiguration {

    private final RestTemplate restTemplate;

    public RdmWebConfiguration(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void configureRestTemplate() {
        restTemplate.getMessageConverters().removeIf(converter ->
            converter instanceof GenericHttpMessageConverter &&
            converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_XML)
        );
    }
}
