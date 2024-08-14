package ru.i_novus.ms.rdm.loader.client.autoconfigure;

import net.n2oapp.platform.loader.autoconfigure.ClientLoaderAutoConfiguration;
import net.n2oapp.platform.loader.client.ClientLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestOperations;
import ru.i_novus.ms.rdm.loader.client.service.RefBookDataClientLoader;
import ru.i_novus.ms.rdm.loader.client.service.RefBookDataRestTemplateCustomizer;

@AutoConfiguration
@ConditionalOnClass(ClientLoader.class)
@ConditionalOnProperty(value = "rdm.loader.client.enabled", matchIfMissing = true)
@AutoConfigureBefore(ClientLoaderAutoConfiguration.class)
@ComponentScan(basePackages = "ru.i_novus.ms.rdm.loader.client")
@SuppressWarnings("unused")
public class RefBookDataClientLoaderAutoConfiguration {

    private static final String ENDPOINT_PATTERN = "/loaders/{subject}/{target}";

    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("I-novus:MethodNameWordCountRule")
    public RefBookDataRestTemplateCustomizer refBookDataRestTemplateCustomizer() {

        return new RefBookDataRestTemplateCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public RefBookDataClientLoader refBookDataClientLoader(@Qualifier("clientLoaderRestTemplate")
                                                           RestOperations clientLoaderRestTemplate) {

        final RefBookDataClientLoader loader = new RefBookDataClientLoader(clientLoaderRestTemplate);
        loader.setEndpointPattern(ENDPOINT_PATTERN);

        return loader;
    }
}
