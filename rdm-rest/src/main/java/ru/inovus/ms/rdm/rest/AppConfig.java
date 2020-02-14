package ru.inovus.ms.rdm.rest;

import net.n2oapp.platform.loader.server.ServerLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import ru.inovus.ms.rdm.rest.loader.RefBookDataServerLoaderRunner;

import java.util.List;

@Configuration
@ComponentScan({"ru.inovus.ms.rdm.rest", "net.n2oapp.platform.loader.server"})
@SuppressWarnings("unused")
public class AppConfig {

    @Bean
    @ConditionalOnMissingBean
    public RefBookDataServerLoaderRunner refBookDataServerLoaderRunner(List<ServerLoader> loaders) {
        return new RefBookDataServerLoaderRunner(loaders);
    }

    @Bean
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(20000000);
        resolver.setResolveLazily(false);
        return resolver;
    }
}
