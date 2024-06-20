package ru.i_novus.ms.rdm.rest.client.config.l10n;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.rest.client.feign.l10n.L10nServiceFeignClient;
import ru.i_novus.ms.rdm.rest.client.feign.l10n.VersionLocaleServiceFeignClient;
import ru.i_novus.ms.rdm.rest.client.impl.l10n.L10nServiceRestClient;
import ru.i_novus.ms.rdm.rest.client.impl.l10n.VersionLocaleServiceRestClient;

@Configuration
@EnableFeignClients("ru.i_novus.ms.rdm.rest.client.feign.l10n")
public class L10nRestClientConfiguration {

    @Bean
    public L10nServiceRestClient l10nService(L10nServiceFeignClient client) {
        return new L10nServiceRestClient(client);
    }

    @Bean
    public VersionLocaleServiceRestClient versionLocaleService(VersionLocaleServiceFeignClient client) {
        return new VersionLocaleServiceRestClient(client);
    }
}
