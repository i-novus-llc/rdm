package ru.i_novus.ms.rdm.rest.autoconfigure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.rest.async.AsyncOperationSynchroSender;

/**
 * Асинхронные операции: Конфигурация для использования синхронных вызовов.
 */
@Configuration
@ConditionalOnProperty(name = "rdm.enable.async.operation", havingValue = "false", matchIfMissing = false)
@SuppressWarnings({"unused","I-novus:MethodNameWordCountRule"})
public class AsyncOperationSynchroConfiguration {

    @Autowired
    public AsyncOperationSynchroConfiguration() {
        // Nothing to do.
    }

    @Bean
    public AsyncOperationSynchroSender synchroAsyncOperationSender() {
        return new AsyncOperationSynchroSender();
    }

}
