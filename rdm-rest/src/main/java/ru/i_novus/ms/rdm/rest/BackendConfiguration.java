package ru.i_novus.ms.rdm.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.LocalDateTimeISOParameterConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import ru.i_novus.ms.audit.client.SourceApplicationAccessor;
import ru.i_novus.ms.audit.client.UserAccessor;
import ru.i_novus.ms.rdm.api.provider.*;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.util.json.LocalDateTimeMapperPreparer;
import ru.i_novus.ms.rdm.rest.provider.StaleStateExceptionMapper;
import ru.i_novus.ms.rdm.rest.service.PublishListener;
import ru.i_novus.ms.rdm.rest.util.SecurityContextUtils;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;

@Configuration
@SuppressWarnings({"unused","I-novus:MethodNameWordCountRule"})
public class BackendConfiguration {

    @Autowired
    private FieldFactory fieldFactory;

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Autowired
    @Qualifier("cxfObjectMapper")
    private ObjectMapper objectMapper;

    @Bean
    MskUtcLocalDateTimeParamConverter mskUtcLocalDateTimeParamConverter() {
        return new MskUtcLocalDateTimeParamConverter(new LocalDateTimeISOParameterConverter());
    }

    @Bean
    public AttributeFilterConverter attributeFilterConverter() {
        return new AttributeFilterConverter();
    }

    @Bean
    public OffsetDateTimeParamConverter offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    LocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new LocalDateTimeMapperPreparer();
    }

    @Bean
    ExportFileProvider exportFileProvider(){
        return new ExportFileProvider();
    }

    @Bean
    RdmMapperConfigurer rdmMapperConfigurer(){
        return new RdmMapperConfigurer();
    }

    @Bean
    @ConditionalOnClass(Messages.class)
    NotFoundExceptionMapper notFoundExceptionMapper(Messages messages) {
        return new NotFoundExceptionMapper(messages);
    }

    @Bean
    @ConditionalOnClass(Messages.class)
    IllegalArgumentExceptionMapper illegalArgumentExceptionMapper(Messages messages) {
        return new IllegalArgumentExceptionMapper(messages);
    }

    @Bean
    @ConditionalOnClass(Messages.class)
    StaleStateExceptionMapper staleStateExceptionMapper(Messages messages) {
        return new StaleStateExceptionMapper(messages);
    }

    @Bean
    @Primary
    @ConditionalOnClass(Messages.class)
    UserExceptionMapper userExceptionMapper(Messages messages) {
        return new UserExceptionMapper(messages);
    }

    @Bean
    @Qualifier("topicJmsTemplate")
    @ConditionalOnProperty(name = "rdm.enable.publish.topic", havingValue = "true")
    public JmsTemplate topicJmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setPubSubDomain(true);
        jmsTemplate.setExplicitQosEnabled(true);
        long oneHour = 60 * 60000L;
        jmsTemplate.setTimeToLive(oneHour);
        return jmsTemplate;
    }

    @Bean
    @Qualifier("queueJmsTemplate")
    public JmsTemplate queueJmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    public DefaultJmsListenerContainerFactory internalAsyncOperationContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "rdm.enable.publish.topic", havingValue = "true")
    public DefaultJmsListenerContainerFactory publishTopicListenerContainerFactory(ConnectionFactory connectionFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(false);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "rdm.enable.publish.topic", havingValue = "true")
    public PublishListener publishListener() {
        return new PublishListener();
    }

    @Bean
    public UserAccessor userAccessor() {
        return () -> createAuditUser(SecurityContextUtils.getUserId(), SecurityContextUtils.getUserName());
    }

    private ru.i_novus.ms.audit.client.model.User createAuditUser(String id, String name) {
        return new ru.i_novus.ms.audit.client.model.User(id != null ? id : name, name);
    }

    @Bean
    @Value("${rdm.audit.application.name}")
    public SourceApplicationAccessor applicationAccessor(String appName) {
        return () -> appName;
    }

    @PostConstruct
    @SuppressWarnings("java:S2696")
    public void setUpObjectMapper() {
        JsonUtil.jsonMapper = objectMapper;
    }
}
