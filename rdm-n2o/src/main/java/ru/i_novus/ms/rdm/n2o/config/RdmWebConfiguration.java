package ru.i_novus.ms.rdm.n2o.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Configuration
@Import(value = { ClientConfiguration.class, N2oConfiguration.class })
@ComponentScan(basePackages = "ru.i_novus.ms.rdm.n2o")
public class RdmWebConfiguration {

    private final RestTemplate restTemplate;

    private final ClientHttpRequestInterceptor userinfoRestTemplateInterceptor;

    @Autowired
    public RdmWebConfiguration(RestTemplate restTemplate,
                               @Qualifier("userinfoRestTemplateInterceptor")
                               ClientHttpRequestInterceptor userinfoRestTemplateInterceptor) {
        this.restTemplate = restTemplate;
        this.userinfoRestTemplateInterceptor = userinfoRestTemplateInterceptor;
    }

    @PostConstruct
    public void configureRestTemplate() {

        restTemplate.getMessageConverters().removeIf(converter ->
            converter instanceof GenericHttpMessageConverter &&
            converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_XML)
        );

        restTemplate.getInterceptors().add(userinfoRestTemplateInterceptor);
    }
}
