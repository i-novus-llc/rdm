package ru.i_novus.ms.rdm.config;

import org.apache.cxf.bus.spring.SpringBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.MBeanExporter;

@Configuration
@Profile("!test")
@ConditionalOnClass({ MBeanExporter.class })
@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true")
public class CxfConfiguration {

    /**
     * Создаем SpringBus после mbeanExporter, иначе ошибка
     * в {@link org.apache.cxf.management.jmx.InstrumentationManagerImpl#init()}
     */
    @Bean
    @DependsOn("mbeanExporter")
    public SpringBus cxf() {
        return new SpringBus();
    }

}
