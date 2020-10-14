package ru.i_novus.ms.rdm.loader.client.autoconfigure;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Настройщик RestTemplate для отправки `MultiValueMap`.
 * @see ru.i_novus.ms.rdm.loader.client.loader.RefBookDataClientLoader
 */
public class RefBookDataRestTemplateCustomizer implements RestTemplateCustomizer {

    @Override
    public void customize(RestTemplate restTemplate) {

        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        if (converters.stream().anyMatch(converter -> converter instanceof FormHttpMessageConverter))
            return;

        converters.add(new FormHttpMessageConverter());
    }
}
