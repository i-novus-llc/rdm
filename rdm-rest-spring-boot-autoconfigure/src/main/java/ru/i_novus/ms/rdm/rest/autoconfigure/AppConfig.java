package ru.i_novus.ms.rdm.rest.autoconfigure;

import jakarta.servlet.MultipartConfigElement;
import net.n2oapp.platform.loader.server.ServerLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
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
    public RefBookDataServerLoaderRunner refBookDataServerLoaderRunner(List<ServerLoader> loaders,
                                                                       FileStorageService fileStorageService) {
        return new RefBookDataServerLoaderRunner(loaders, fileStorageService);
    }

    @Bean
    public MultipartResolver multipartResolver() {

        final StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        resolver.setResolveLazily(false);
        return resolver;
    }

    @Bean
    @SuppressWarnings("java:S5693")
    public MultipartConfigElement multipartConfigElement() {

        final MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(maxFileSize)); // spring.servlet.multipart.max-file-size
        return factory.createMultipartConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate platformRestTemplate() {
        return new RestTemplate(); // for BackendConfiguration
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientHttpRequestInterceptor userinfoRestTemplateInterceptor() {
        return (request, body, execution) -> execution.execute(request, body); // for BackendConfiguration
    }
}
