package ru.i_novus.ms.rdm.rest.autoconfigure.config;

import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Асинхронные операции: Конфигурация для использования JMS.
 */
@Configuration
@ConditionalOnProperty(name = "rdm.enable.async.operation", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = {
        "ru.i_novus.ms.rdm.async.impl", // rdm-async-impl
})
@SuppressWarnings({"unused","I-novus:MethodNameWordCountRule"})
public class AsyncOperationJmsConfiguration {

    @Autowired
    public AsyncOperationJmsConfiguration() {
        // Nothing to do.
    }

    // Асинхронные операции.

    @Bean
    @Qualifier("queueJmsTemplate")
    public JmsTemplate queueJmsTemplate(
            @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory
    ) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    public DefaultJmsListenerContainerFactory internalAsyncOperationContainerFactory(
            @Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory
    ) {
        final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);

        return factory;
    }

}
