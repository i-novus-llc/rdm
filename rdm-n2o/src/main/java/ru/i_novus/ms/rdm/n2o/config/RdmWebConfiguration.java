package ru.i_novus.ms.rdm.n2o.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
@SuppressWarnings("FieldCanBeLocal")
public class RdmWebConfiguration {

    private final RestTemplate platformRestTemplate;

    private final RestTemplate restProviderRestTemplate;

    private final ClientHttpRequestInterceptor userinfoRestTemplateInterceptor;

    /* From N2oEngineConfiguration */
    @Value("${n2o.engine.rest.url}")
    private String baseRestUrl;

    @Value("${n2o.engine.rest.dateformat.serialize}")
    private String serializingFormat;

    @Value("${n2o.engine.rest.dateformat.deserialize}")
    private String[] deserializingFormats;

    @Value("${n2o.engine.rest.dateformat.exclusion-keys}")
    private String[] exclusionKeys;

    @Autowired
    public RdmWebConfiguration(@Qualifier("platformRestTemplate") RestTemplate platformRestTemplate,
                               @Qualifier("restProviderRestTemplate") RestTemplate restProviderRestTemplate,
                               @Qualifier("userinfoRestTemplateInterceptor")
                               ClientHttpRequestInterceptor userinfoRestTemplateInterceptor) {
        this.platformRestTemplate = platformRestTemplate;
        this.restProviderRestTemplate = restProviderRestTemplate;
        this.userinfoRestTemplateInterceptor = userinfoRestTemplateInterceptor;
    }

    @PostConstruct
    public void configureRestTemplate() {

        platformRestTemplate.getMessageConverters().removeIf(converter ->
            converter instanceof GenericHttpMessageConverter &&
            converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_XML)
        );

        restProviderRestTemplate.getInterceptors().add(userinfoRestTemplateInterceptor);
    }
}
