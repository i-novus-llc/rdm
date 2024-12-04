package ru.i_novus.ms.rdm.rest.autoconfigure;

import net.n2oapp.platform.loader.server.ServerLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import ru.i_novus.ms.rdm.api.service.FileStorageService;
import ru.i_novus.ms.rdm.rest.loader.RefBookDataServerLoaderRunner;

import java.util.List;

@Configuration
@SuppressWarnings({"unused", "rawtypes", "java:S3740"})
public class AppConfig {

    @Value("${rdm.loader.max.file-size:50000000}")
    private long maxFileSize;

    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("I-novus:MethodNameWordCountRule")
    public RefBookDataServerLoaderRunner refBookDataServerLoaderRunner(
            List<ServerLoader> loaders
    ) {
        return new RefBookDataServerLoaderRunner(loaders);
    }

    @Bean
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(maxFileSize);
        resolver.setResolveLazily(false);
        return resolver;
    }
}
