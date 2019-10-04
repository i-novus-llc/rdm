package ru.inovus.ms.rdm;

import net.n2oapp.platform.jaxrs.autoconfigure.EnableJaxRsProxyClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.inovus.ms.rdm.n2o.ClientConfiguration;
import ru.inovus.ms.rdm.api.util.RdmPermission;
import ru.inovus.ms.rdm.util.RdmPermissionImpl;

@Configuration
@EnableJaxRsProxyClient(
        scanPackages = "ru.inovus.ms.rdm.api.service",
        address = "${rdm.backend.path}"
)
public class FrontendConfiguration extends ClientConfiguration {

    @Bean
    @Primary
    @Override
    public RdmPermission rdmPermission() {
        return new RdmPermissionImpl();
    }
}
