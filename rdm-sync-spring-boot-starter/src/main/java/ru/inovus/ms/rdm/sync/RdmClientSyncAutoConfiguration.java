package ru.inovus.ms.rdm.sync;

import liquibase.integration.spring.SpringLiquibase;
import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.StringUtils;
import ru.inovus.ms.rdm.provider.ExportFileProvider;
import ru.inovus.ms.rdm.provider.RdmParamConverterProvider;
import ru.inovus.ms.rdm.provider.RowValueMapperPreparer;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;
import ru.inovus.ms.rdm.sync.service.*;
import ru.inovus.ms.rdm.util.json.LocalDateTimeMapperPreparer;

import javax.sql.DataSource;

/**
 * @author lgalimova
 * @since 20.02.2019
 */
@Configuration
@ConditionalOnClass(RdmSyncRestImpl.class)
@EnableConfigurationProperties(RdmClientSyncProperties.class)
@EnableJaxRsProxyClient(
        classes = {RefBookService.class, VersionService.class, CompareService.class},
        address = "${rdm.client.sync.url}"
)
@AutoConfigureAfter(LiquibaseAutoConfiguration.class)
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
    @DependsOn("liquibase")
    public SpringLiquibase liquibaseRdm(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setDatabaseChangeLogLockTable("databasechangeloglock_rdms");
        liquibase.setChangeLog("classpath*:/rdm-sync-db/baseChangelog.xml");
        return liquibase;
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
    LocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new LocalDateTimeMapperPreparer();
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
