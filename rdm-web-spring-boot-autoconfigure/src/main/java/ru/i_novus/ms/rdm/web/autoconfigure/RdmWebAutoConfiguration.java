package ru.i_novus.ms.rdm.web.autoconfigure;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.i_novus.ms.rdm.api.provider.ExportFileProvider;
import ru.i_novus.ms.rdm.api.provider.RdmMapperConfigurer;
import ru.i_novus.ms.rdm.n2o.config.ClientConfiguration;
import ru.i_novus.ms.rdm.n2o.config.N2oConfiguration;
import ru.i_novus.ms.rdm.n2o.config.UiStrategyLocatorConfig;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.util.RefBookAdapter;

@AutoConfiguration
@ConditionalOnProperty(name = "rdm.backend.path")
@ComponentScan({
        "ru.i_novus.ms.rdm.n2o.service",  "ru.i_novus.ms.rdm.n2o.validation",
        "ru.i_novus.ms.rdm.n2o.strategy", "ru.i_novus.ms.rdm.n2o.resolver",
        "ru.i_novus.ms.rdm.n2o.provider", "ru.i_novus.ms.rdm.n2o.transformer"
})
@Import(UiStrategyLocatorConfig.class)
@AutoConfigureAfter({ WebMvcAutoConfiguration.class })
@SuppressWarnings({"java:S3740"})
public class RdmWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExportFileProvider exportFileProvider() {
        return new ExportFileProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public RdmMapperConfigurer rdmMapperConfigurer() {
        return new RdmMapperConfigurer();
    }

    @Bean
    @ConditionalOnMissingBean
    public RefBookAdapter refBookAdapter(UiStrategyLocator strategyLocator, Messages messages) {
        return new RefBookAdapter(strategyLocator, messages);
    }

    @Configuration
    static class RdmClientConfiguration extends ClientConfiguration {
    }

    @Configuration
    static class RdmN2oConfiguration extends N2oConfiguration {
    }
}
