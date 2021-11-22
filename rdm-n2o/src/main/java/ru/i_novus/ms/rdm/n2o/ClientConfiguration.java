package ru.i_novus.ms.rdm.n2o;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.api.provider.AttributeFilterConverter;
import ru.i_novus.ms.rdm.api.provider.OffsetDateTimeParamConverter;
import ru.i_novus.ms.rdm.api.util.RdmPermission;
import ru.i_novus.ms.rdm.n2o.util.RdmPermissionImpl;
import ru.i_novus.ms.rdm.n2o.util.json.RdmN2oLocalDateTimeMapperPreparer;

@Configuration
@EnableJaxRsProxyClient(
        scanPackages = "ru.i_novus.ms.rdm.api.rest, ru.i_novus.ms.rdm.api.service",
        address = "${rdm.backend.path}"
)
public class ClientConfiguration {

    @Bean
    public RdmPermission rdmPermission() {
        return new RdmPermissionImpl();
    }

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
}

