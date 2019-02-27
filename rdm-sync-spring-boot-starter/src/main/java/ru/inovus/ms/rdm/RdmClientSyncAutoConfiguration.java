package ru.inovus.ms.rdm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.service.RdmSyncRest;
import ru.inovus.ms.rdm.service.RdmSyncRestImpl;

/**
 * @author lgalimova
 * @since 20.02.2019
 */
@Configuration
@ConditionalOnClass(RdmSyncRest.class)
@EnableConfigurationProperties(RdmClientSyncProperties.class)
public class RdmClientSyncAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public RdmSyncRest rdmSyncService() {
        return new RdmSyncRestImpl();
    }
}
