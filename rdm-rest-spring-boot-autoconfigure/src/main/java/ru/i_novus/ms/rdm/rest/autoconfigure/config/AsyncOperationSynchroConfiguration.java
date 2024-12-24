package ru.i_novus.ms.rdm.rest.autoconfigure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Асинхронные операции: Конфигурация для использования синхронных вызовов.
 */
@Configuration
@ConditionalOnProperty(name = "rdm.enable.async.operation", havingValue = "false", matchIfMissing = false)
@ComponentScan(basePackages = {
        "ru.i_novus.ms.rdm.async.impl", // rdm-async-impl
})
@SuppressWarnings({"unused","I-novus:MethodNameWordCountRule"})
public class AsyncOperationSynchroConfiguration {

    @Autowired
    public AsyncOperationSynchroConfiguration() {
        // Nothing to do.
    }

}
