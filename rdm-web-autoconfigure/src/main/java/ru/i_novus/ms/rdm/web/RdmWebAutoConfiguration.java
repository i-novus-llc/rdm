package ru.i_novus.ms.rdm.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import ru.i_novus.ms.rdm.api.provider.*;
import ru.i_novus.ms.rdm.api.util.RdmPermission;
import ru.i_novus.ms.rdm.n2o.UiStrategyLocatorConfig;
import ru.i_novus.ms.rdm.n2o.service.RefBookController;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.util.RdmPermissionImpl;
import ru.i_novus.ms.rdm.n2o.util.RefBookAdapter;
import ru.i_novus.ms.rdm.n2o.util.json.RdmN2oLocalDateTimeMapperPreparer;

@Configuration
@ConditionalOnClass(RefBookController.class)
@ConditionalOnProperty(name = "rdm.backend.path")
@ComponentScan({
        "ru.i_novus.ms.rdm.n2o.service", "ru.i_novus.ms.rdm.n2o.strategy",
        "ru.i_novus.ms.rdm.n2o.provider", "ru.i_novus.ms.rdm.n2o.resolver",
        "ru.i_novus.ms.rdm.n2o.transformer"
})
@EnableJaxRsProxyClient(
        scanPackages = "ru.i_novus.ms.rdm.api.rest, ru.i_novus.ms.rdm.api.service",
        address = "${rdm.backend.path}"
)
@Import(UiStrategyLocatorConfig.class)
public class RdmWebAutoConfiguration {

    @Bean
    public AttributeFilterConverter attributeFilterConverter(
            @Autowired @Qualifier("cxfObjectMapper") ObjectMapper objectMapper
    ) {
        return new AttributeFilterConverter(objectMapper);
    }

    @Bean
    public OffsetDateTimeParamConverter offsetDateTimeParamConverter() {
        return new OffsetDateTimeParamConverter();
    }

    @Bean
    public RdmN2oLocalDateTimeMapperPreparer localDateTimeMapperPreparer() {
        return new RdmN2oLocalDateTimeMapperPreparer();
    }

    @Bean
    public ExportFileProvider exportFileProvider() {
        return new ExportFileProvider();
    }

    @Bean
    public RdmMapperConfigurer rdmMapperConfigurer() {
        return new RdmMapperConfigurer();
    }

    @Bean
    public RdmPermission rdmPermission() {
        return new RdmPermissionImpl();
    }

    @Bean
    public RefBookAdapter refBookAdapter(UiStrategyLocator strategyLocator, Messages messages) {
        return new RefBookAdapter(strategyLocator, messages);
    }
}
