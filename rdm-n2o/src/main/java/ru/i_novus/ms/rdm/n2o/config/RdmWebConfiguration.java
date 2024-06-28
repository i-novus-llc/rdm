package ru.i_novus.ms.rdm.n2o.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.framework.api.rest.RestLoggingHandler;
import net.n2oapp.framework.engine.data.rest.SpringRestDataProviderEngine;
import net.n2oapp.framework.engine.data.rest.json.RestEngineTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.List;

@Configuration
@Import(value = { ClientConfiguration.class, N2oConfiguration.class })
@ComponentScan(basePackages = "ru.i_novus.ms.rdm.n2o")
public class RdmWebConfiguration {

    private final RestTemplate restTemplate;

    private final ClientHttpRequestInterceptor userinfoRestTemplateInterceptor;

    @Value("${n2o.engine.rest.url}")
    private String baseRestUrl;

    @Value("${n2o.engine.rest.dateformat.serialize}")
    private String serializingFormat;

    @Value("${n2o.engine.rest.dateformat.deserialize}")
    private String[] deserializingFormats;

    @Value("${n2o.engine.rest.dateformat.exclusion-keys}")
    private String[] exclusionKeys;

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
    }

    @Bean
    public SpringRestDataProviderEngine restDataProviderEngine(RestTemplateBuilder builder, List<RestLoggingHandler> loggingHandlers) {
        ObjectMapper restObjectMapper = restObjectMapper();
        SpringRestDataProviderEngine springRestDataProviderEngine = new SpringRestDataProviderEngine(
                rdmRestTemplate(builder, httpMessageConverter(restObjectMapper)),
                restObjectMapper, loggingHandlers);

        springRestDataProviderEngine.setBaseRestUrl(baseRestUrl);
        return springRestDataProviderEngine;
    }

    private ObjectMapper restObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat(serializingFormat));
        RestEngineTimeModule module = new RestEngineTimeModule(deserializingFormats, exclusionKeys);
        objectMapper.registerModules(module);
        return objectMapper;
    }

    private MappingJackson2HttpMessageConverter httpMessageConverter(ObjectMapper restObjectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(restObjectMapper);
        return converter;
    }

    private RestTemplate rdmRestTemplate(RestTemplateBuilder builder, MappingJackson2HttpMessageConverter converter) {
        DefaultUriBuilderFactory builderFactory = new DefaultUriBuilderFactory();
        builderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);
        RestTemplate restTemplate = builder.messageConverters(converter).build();
        restTemplate.setUriTemplateHandler(builderFactory);
        restTemplate.getInterceptors().add(userinfoRestTemplateInterceptor);
        return restTemplate;
    }
}
