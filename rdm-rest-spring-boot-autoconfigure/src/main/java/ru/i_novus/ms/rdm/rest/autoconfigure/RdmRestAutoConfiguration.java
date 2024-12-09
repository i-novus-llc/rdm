package ru.i_novus.ms.rdm.rest.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.i_novus.ms.rdm.rest.autoconfigure.config.AppConfig;
import ru.i_novus.ms.rdm.rest.autoconfigure.config.BackendConfiguration;

@AutoConfiguration
@ComponentScan(basePackages = {
        "ru.i_novus.ms.rdm.rest.autoconfigure.config", // configs
        "ru.i_novus.ms.rdm.rest", // rdm-rest
        "ru.i_novus.ms.rdm.impl", // rdm-impl
        "ru.i_novus.ms.rdm.l10n.impl", // l10n-impl
        "ru.i_novus.ms.rdm.api.audit.impl", // deprecated
        "ru.i_novus.platform.versioned_data_storage.config", // vds, l10n-vds
})
@Import(value = {
        AppConfig.class,
        BackendConfiguration.class
})
@EnableJpaRepositories("ru.i_novus.ms.rdm.impl.repository")
@EntityScan(basePackages = "ru.i_novus.ms.rdm")
@PropertySource("classpath:/rdm-rest-default.properties")
public class RdmRestAutoConfiguration {

}
