package ru.inovus.ms.rdm.rest;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.LocalDateTimeISOParameterConverter;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import ru.i_novus.ms.audit.client.SourceApplicationAccessor;
import ru.i_novus.ms.audit.client.UserAccessor;
import ru.i_novus.ms.audit.client.model.User;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.api.provider.*;
import ru.inovus.ms.rdm.api.util.FileNameGenerator;
import ru.inovus.ms.rdm.api.util.json.LocalDateTimeMapperPreparer;
import ru.inovus.ms.rdm.rest.util.SecurityContextUtils;

import javax.jms.ConnectionFactory;

@Configuration
public class BackendConfiguration {

    @Autowired
    private FieldFactory fieldFactory;

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

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

    @Bean("fnsiFileNameGenerator")
    @Primary
    @ConditionalOnProperty(name = "rdm.download.name-generator-class", havingValue = "FnsiFileNameGenerator")
    public FileNameGenerator fnsiFileNameGenerator(){
        return new FnsiFileNameGenerator();
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
    @Primary
    @ConditionalOnClass(Messages.class)
    UserExceptionMapper userExceptionMapper(Messages messages) {
        return new UserExceptionMapper(messages);
    }

    @Bean
    @ConditionalOnExpression(value = "#{${rdm.enable.publish.topic}}")
    public ConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
        activeMQConnectionFactory.setBrokerURL(brokerUrl);
        return new CachingConnectionFactory(activeMQConnectionFactory);
    }

    @Bean
    @Qualifier("topicJmsTemplate")
    @ConditionalOnExpression(value = "#{${rdm.enable.publish.topic}}")
    public JmsTemplate topicJmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(activeMQConnectionFactory());
        jmsTemplate.setPubSubDomain(true);
        return jmsTemplate;
    }

    @Bean
    public UserAccessor userAccessor() {
        return () -> new User(null, SecurityContextUtils.getUserName());
    }

    @Bean
    @Value("${rdm.audit.application.name}")
    public SourceApplicationAccessor applicationAccessor(String appName) {
        return () -> appName;
    }

}
