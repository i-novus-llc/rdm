package ru.i_novus.ms.rdm.n2o.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.api.provider.AttributeFilterConverter;
import ru.i_novus.ms.rdm.api.provider.OffsetDateTimeParamConverter;
import ru.i_novus.ms.rdm.api.util.RdmPermission;
import ru.i_novus.ms.rdm.n2o.util.RdmPermissionImpl;
import ru.i_novus.ms.rdm.n2o.util.json.RdmN2oLocalDateTimeMapperPreparer;

import java.util.List;

@Configuration
@EnableJaxRsProxyClient(
        scanPackages = {
                "ru.i_novus.ms.rdm.api.rest",
                "ru.i_novus.ms.rdm.api.service"
        },
        address = "${rdm.backend.path}"
)
public class ClientConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RdmPermission rdmPermission() {
        return new RdmPermissionImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public AttributeFilterConverter attributeFilterConverter(
            @Autowired @Qualifier("cxfObjectMapper") ObjectMapper objectMapper
    ) {
        return new AttributeFilterConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public OffsetDateTimeParamConverter offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmN2oLocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new RdmN2oLocalDateTimeMapperPreparer();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate platformRestTemplate(
            @Qualifier("userinfoClientHttpRequestInterceptor") ClientHttpRequestInterceptor interceptor
    ) {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(interceptor));

        return restTemplate; // for RdmWebConfiguration
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientHttpRequestInterceptor userinfoClientHttpRequestInterceptor() {
        return (request, body, execution) ->
                execution.execute(request, body); // for RdmWebConfiguration
    }
}

