package ru.i_novus.ms.rdm.rest.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.jms.ConnectionFactory;
import net.n2oapp.framework.security.autoconfigure.userinfo.UserInfoModel;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.LocalDateTimeISOParameterConverter;
import net.n2oapp.platform.jaxrs.MessageExceptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;
import ru.i_novus.ms.rdm.api.audit.SourceApplicationAccessor;
import ru.i_novus.ms.rdm.api.audit.UserAccessor;
import ru.i_novus.ms.rdm.api.audit.model.User;
import ru.i_novus.ms.rdm.api.provider.*;
import ru.i_novus.ms.rdm.api.util.json.LocalDateTimeMapperPreparer;
import ru.i_novus.ms.rdm.rest.provider.StaleStateExceptionMapper;
import ru.i_novus.ms.rdm.rest.service.PublishListener;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;

import static ru.i_novus.ms.rdm.rest.autoconfigure.SecurityContextUtils.DEFAULT_USER_ID;
import static ru.i_novus.ms.rdm.rest.autoconfigure.SecurityContextUtils.DEFAULT_USER_NAME;

@Configuration
@SuppressWarnings({"unused","FieldCanBeLocal","I-novus:MethodNameWordCountRule"})
public class BackendConfiguration {

    private final RestTemplate platformRestTemplate;

    private final ClientHttpRequestInterceptor userinfoRestTemplateInterceptor;

    private final FieldFactory fieldFactory; // Для десериализации объектов сторонних классов

    @Autowired
    public BackendConfiguration(@Qualifier("platformRestTemplate")
                                        RestTemplate platformRestTemplate,
                                @Qualifier("userinfoRestTemplateInterceptor")
                                    ClientHttpRequestInterceptor userinfoRestTemplateInterceptor,
                                FieldFactory fieldFactory) {
        this.platformRestTemplate = platformRestTemplate;
        this.userinfoRestTemplateInterceptor = userinfoRestTemplateInterceptor;

        this.fieldFactory = fieldFactory;
    }

    @PostConstruct
    private void configureRestTemplate() {

        platformRestTemplate.getInterceptors().add(userinfoRestTemplateInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    MskUtcLocalDateTimeParamConverter mskUtcLocalDateTimeParamConverter() {
        return new MskUtcLocalDateTimeParamConverter(new LocalDateTimeISOParameterConverter());
    }

    @Bean
    public AttributeFilterConverter attributeFilterConverter(
            @Autowired @Qualifier("cxfObjectMapper") ObjectMapper objectMapper
    ) {
        return new AttributeFilterConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public OffsetDateTimeParamConverter offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new LocalDateTimeMapperPreparer();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportFileProvider exportFileProvider(){
        return new ExportFileProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmMapperConfigurer rdmMapperConfigurer(){
        return new RdmMapperConfigurer();
    }

    @Bean
    @ConditionalOnClass(Messages.class)
    public NotFoundExceptionMapper notFoundExceptionMapper(Messages messages) {
        return new NotFoundExceptionMapper(messages);
    }

    @Bean
    @ConditionalOnClass(Messages.class)
    public IllegalArgumentExceptionMapper illegalArgumentExceptionMapper(Messages messages) {
        return new IllegalArgumentExceptionMapper(messages);
    }

    @Bean
    @ConditionalOnClass(Messages.class)
    public StaleStateExceptionMapper staleStateExceptionMapper(Messages messages) {
        return new StaleStateExceptionMapper(messages);
    }

    @Bean
    @Primary
    @ConditionalOnClass(Messages.class)
    public MessageExceptionMapper messageExceptionMapper(Messages messages) {
        return new MessageExceptionMapper(messages);
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
    @ConditionalOnMissingBean
    public UserAccessor userAccessor() {
        return this::createUserAccessor;
    }

    private User createUserAccessor() {

        final Object principal = SecurityContextUtils.getPrincipal();
        if (principal == null)
            return createAuditUser(DEFAULT_USER_ID, DEFAULT_USER_NAME);

        if (principal instanceof UserInfoModel user)
            return createAuditUser(user.email, user.username);

        return createAuditUser("" + principal, DEFAULT_USER_NAME);
    }

    private User createAuditUser(String id, String name) {
        return new User(id != null ? id : name, name);
    }

    @Bean
    @ConditionalOnMissingBean
    public SourceApplicationAccessor applicationAccessor(
            @Value("${rdm.audit.application.name:rdm}") String appName
    ) {
        return () -> appName;
    }
}
