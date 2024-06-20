package ru.i_novus.ms.rdm.rest.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.rest.client.feign.*;
import ru.i_novus.ms.rdm.rest.client.impl.*;

@Configuration
@EnableFeignClients("ru.i_novus.ms.rdm.rest.client.feign")
public class RestClientConfiguration {

    @Bean
    public RefBookServiceRestClient refBookService(RefBookServiceFeignClient client) {
        return new RefBookServiceRestClient(client);
    }

    @Bean
    public VersionRestServiceRestClient versionService(VersionRestServiceFeignClient client) {
        return new VersionRestServiceRestClient(client);
    }

    @Bean
    public DraftRestServiceRestClient draftService(DraftRestServiceFeignClient client) {
        return new DraftRestServiceRestClient(client);
    }

    @Bean
    public PublishServiceRestClient publishService(PublishServiceFeignClient client) {
        return new PublishServiceRestClient(client);
    }

    @Bean
    public ReferenceServiceRestClient referenceService(ReferenceServiceFeignClient client) {
        return new ReferenceServiceRestClient(client);
    }

    @Bean
    public ConflictServiceRestClient conflictService(ConflictServiceFeignClient client) {
        return new ConflictServiceRestClient(client);
    }

    @Bean
    public CompareServiceRestClient compareService(CompareServiceFeignClient client) {
        return new CompareServiceRestClient(client);
    }

    @Bean
    public FileStorageServiceRestClient fileStorageService(FileStorageServiceFeignClient client) {
        return new FileStorageServiceRestClient(client);
    }
}
