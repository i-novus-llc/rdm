package ru.inovus.ms.rdm.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import ru.inovus.ms.rdm.provider.ExportFileProvider;
import ru.inovus.ms.rdm.provider.RdmParamConverterProvider;
import ru.inovus.ms.rdm.provider.RowValueMapperPreparer;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;
import ru.inovus.ms.rdm.sync.service.*;

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
    public RdmClientSyncConfig rdmClientSyncConfig(RdmClientSyncProperties properties) {
        String url = properties.getUrl();
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Rdm client synchronizer properties not configured properly: url is missing");
        }
        RdmClientSyncConfig config = new RdmClientSyncConfig();
        config.put("url", url);
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmSyncRest rdmSyncRest() {
        return new RdmSyncRestImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmMappingService rdmMappingService() {
        return new RdmMappingServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmLoggingService rdmLoggingService() {
        return new RdmLoggingService();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmSyncDao rdmSyncDao() {
        return new RdmSyncDaoImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    RdmParamConverterProvider rdmParamConverterProvider() {
        return new RdmParamConverterProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    ExportFileProvider exportFileProvider(){
        return new ExportFileProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    RowValueMapperPreparer rowValueMapperPreparer(){
        return new RowValueMapperPreparer();
    }
}
