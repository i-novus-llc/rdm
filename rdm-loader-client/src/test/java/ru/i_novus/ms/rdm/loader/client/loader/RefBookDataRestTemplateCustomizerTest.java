package ru.i_novus.ms.rdm.loader.client.loader;

import org.junit.Test;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class RefBookDataRestTemplateCustomizerTest {

    @Test
    public void testClass() {

        RefBookDataRestTemplateCustomizer customizer = new RefBookDataRestTemplateCustomizer();

        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);

        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        assertTrue(converters.stream().anyMatch(converter -> converter instanceof FormHttpMessageConverter));
    }
}