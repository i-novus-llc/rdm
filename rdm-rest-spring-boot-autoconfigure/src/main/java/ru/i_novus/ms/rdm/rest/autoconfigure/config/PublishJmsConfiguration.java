package ru.i_novus.ms.rdm.rest.autoconfigure.config;

import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import ru.i_novus.ms.rdm.rest.provider.PublishListener;

/**
 * Вещание событий публикации: Конфигурация для использования JMS.
 */
@Configuration
@SuppressWarnings({"unused","I-novus:MethodNameWordCountRule"})
@ConditionalOnProperty(name = "rdm.enable.publish.topic", havingValue = "true")
public class PublishJmsConfiguration {

    @Autowired
    public PublishJmsConfiguration() {
        // Nothing to do.
    }

    @Bean
    @Qualifier("topicJmsTemplate")
    public JmsTemplate topicJmsTemplate(
            @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory
    ) {
        final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setPubSubDomain(true);
        jmsTemplate.setExplicitQosEnabled(true);

        final long oneHour = 60 * 60000L;
        jmsTemplate.setTimeToLive(oneHour);

        return jmsTemplate;
    }

    @Bean
    public DefaultJmsListenerContainerFactory publishTopicListenerContainerFactory(
            @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory
    ) {
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSubscriptionShared(false);

        return factory;
    }

    @Bean
    public PublishListener publishListener() {
        return new PublishListener();
    }

}
