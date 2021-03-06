package ru.i_novus.ms.rdm.sync;

import liquibase.integration.spring.SpringLiquibase;
import net.n2oapp.platform.jaxrs.LocalDateTimeISOParameterConverter;
import net.n2oapp.platform.jaxrs.TypedParamConverter;
import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import net.n2oapp.platform.jaxrs.autoconfigure.MissingGenericBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.provider.*;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.json.LocalDateTimeMapperPreparer;
import ru.i_novus.ms.rdm.sync.rest.LocalRdmDataService;
import ru.i_novus.ms.rdm.sync.rest.RdmSyncRest;
import ru.i_novus.ms.rdm.sync.service.*;
import ru.i_novus.ms.rdm.sync.service.change_data.*;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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
@EnableJms
@ComponentScan({"ru.i_novus.ms.rdm.sync.service.init"})
@ConditionalOnProperty(
        value="rdm_sync.enabled",
        matchIfMissing = true)
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
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass(value = "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory")
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
    public RdmSyncRest lockingRdmSyncRest() {
        return new LockingRdmSyncRest();
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
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<LocalDateTime> mskUtcLocalDateTimeParamConverter() {
        return new MskUtcLocalDateTimeParamConverter(new LocalDateTimeISOParameterConverter());
    }

    @Bean
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<LocalDate> isoLocaldateParamConverter() {
        return new IsoLocalDateParamConverter();
    }

    @Bean
    @Conditional(MissingGenericBean.class)
    public  TypedParamConverter<AttributeFilter> attributeFilterConverter() {
        return new AttributeFilterConverter();
    }

    @Bean
    @Conditional(MissingGenericBean.class)
    public TypedParamConverter<OffsetDateTime> offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new LocalDateTimeMapperPreparer();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportFileProvider exportFileProvider() {
        return new ExportFileProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmMapperConfigurer rdmMapperConfigurer() {
        return new RdmMapperConfigurer();
    }

    @Bean(name = "publishDictionaryTopicMessageListenerContainerFactory")
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
    @ConditionalOnClass(name = "org.apache.activemq.ActiveMQConnectionFactory")
    public DefaultJmsListenerContainerFactory unsharedPublishContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(false);
        return factory;
    }

    @Bean(name = "publishDictionaryTopicMessageListenerContainerFactory")
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
    @ConditionalOnClass(name = "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory")
    public DefaultJmsListenerContainerFactory sharedPublishContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(true);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(value = "rdm_sync.change_data_mode", havingValue = "async")
    public DefaultJmsListenerContainerFactory rdmChangeDataQueueMessageListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "rdm_sync.publish.listener.enable", havingValue = "true")
    public PublishListener publishListener(RdmSyncRest rdmSyncRest) {
        return new PublishListener(rdmSyncRest);
    }

    @Bean
    @ConditionalOnProperty(name = "rdm_sync.change_data_mode", havingValue = "async")
    public RdmChangeDataListener rdmChangeDataListener(RefBookService refBookService, RdmChangeDataRequestCallback rdmChangeDataRequestCallback) {
        return new RdmChangeDataListener(refBookService, rdmChangeDataRequestCallback);
    }

    @Bean
    @ConditionalOnProperty(value = "rdm_sync.change_data_mode", havingValue = "sync")
    public RdmChangeDataClient syncRdmChangeDataClient() {
        return new SyncRdmChangeDataClient();
    }

    @Bean
    @ConditionalOnProperty(value = "rdm_sync.change_data_mode", havingValue = "async")
    public RdmChangeDataClient asyncRdmChangeDataClient() {
        return new AsyncRdmChangeDataClient();
    }


    @Bean
    public RdmChangeDataRequestCallback rdmChangeDataRequestCallback() {
        return new RdmChangeDataRequestCallback.DefaultRdmChangeDataRequestCallback();
    }

    @Bean
    @ConditionalOnMissingBean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public RdmSyncLocalRowStateService rdmSyncLocalRowStateService() {
        return new RdmSyncLocalRowStateService();
    }

    @Bean
    @ConditionalOnProperty(value = "rdm_sync.change_data_mode", havingValue = "")
    @SuppressWarnings("squid:S2440")
    public RdmSyncJobContext rdmSyncJobContext(RdmSyncDao rdmSyncDao, RdmChangeDataClient rdmChangeDataClient, @Value("${rdm_sync.export_from_local.batch_size:100}") int exportToRdmBatchSize) {
        return new RdmSyncJobContext(rdmSyncDao, rdmChangeDataClient, exportToRdmBatchSize);
    }

    @Bean
    public LocalRdmDataService localRdmDataService() {
        return new LocalRdmDataServiceImpl();
    }

}
