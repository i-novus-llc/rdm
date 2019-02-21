package ru.inovus.ms.rdm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import ru.inovus.ms.rdm.provider.ExportFileProvider;
import ru.inovus.ms.rdm.provider.RdmParamConverterProvider;
import ru.inovus.ms.rdm.provider.RowValueMapperPreparer;
import ru.inovus.ms.rdm.service.*;

/**
 * @author lgalimova
 * @since 20.02.2019
 */
@Configuration
@ConditionalOnClass(RdmSyncRest.class)
@EnableConfigurationProperties(RdmClientSyncProperties.class)
public class RdmClientSyncAutoConfiguration {

    @Autowired
    private RdmClientSyncProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public RdmClientSyncConfig rdmClientSyncConfig() {
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
    public RdmSyncRest rdmSyncRest(RdmClientSyncConfig config) {
        return new RdmSyncRestImpl(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmSyncService rdmSyncService() {
        return new RdmSyncService();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmMapping rdmMapping() {
        return new RdmMappingImpl();
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
