package ru.i_novus.ms.rdm.rest.autoconfigure;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan({"ru.i_novus.ms.rdm", "ru.i_novus.platform.versioned_data_storage.config"})
@Import(value = {AppConfig.class, BackendConfiguration.class})
@EnableJpaRepositories("ru.i_novus.ms.rdm.impl.repository")
@EntityScan(basePackages = "ru.i_novus.ms.rdm")
@PropertySource("classpath:/rdm-rest-default.properties")
public class RdmRestAutoConfiguration {

}
