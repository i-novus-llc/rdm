package ru.i_novus.ms.rdm.rest.autoconfigure;

import net.n2oapp.platform.loader.server.ServerLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import ru.i_novus.ms.rdm.rest.loader.RefBookDataServerLoaderRunner;

import java.util.List;

@Configuration
@ComponentScan({"ru.i_novus.ms.rdm", "ru.i_novus.platform.versioned_data_storage.config"})
@Import(value = {AppConfig.class, BackendConfiguration.class})
@EnableJpaRepositories("ru.i_novus.ms.rdm.impl.repository")
@EntityScan(basePackages = "ru.i_novus.ms.rdm")
@PropertySource("classpath:rdm-rest-default.properties")
public class RdmRestAutoConfiguration {

}
