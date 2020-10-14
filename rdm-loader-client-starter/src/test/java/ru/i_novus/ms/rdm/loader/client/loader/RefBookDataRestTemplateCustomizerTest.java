package ru.i_novus.ms.rdm.loader.client.loader;

import org.junit.Test;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RefBookDataRestTemplateCustomizerTest {

    @Test
    public void testClass() {

        RefBookDataRestTemplateCustomizer customizer = new RefBookDataRestTemplateCustomizer();

        List<HttpMessageConverter<?>> converters = new ArrayList<>(1);
        converters.add(new StringHttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(converters);
        customizer.customize(restTemplate);

        converters = restTemplate.getMessageConverters();
        assertEquals(1, converters.stream().filter(this::isRequiredConverter).count());
    }

    @Test
    public void testWhenConverterExists() {

        RefBookDataRestTemplateCustomizer customizer = new RefBookDataRestTemplateCustomizer();

        List<HttpMessageConverter<?>> converters = new ArrayList<>(1);
        converters.add(new FormHttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(converters);
        customizer.customize(restTemplate);

        converters = restTemplate.getMessageConverters();
        assertEquals(1, converters.stream().filter(this::isRequiredConverter).count());
    }

    private boolean isRequiredConverter(HttpMessageConverter<?> converter) {

        return converter instanceof FormHttpMessageConverter;
    }
}