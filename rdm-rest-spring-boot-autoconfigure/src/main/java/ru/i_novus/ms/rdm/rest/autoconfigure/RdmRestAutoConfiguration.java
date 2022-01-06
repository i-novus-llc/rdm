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
@Import(value = {AppConfig.class, BackendConfiguration.class})
@EnableJpaRepositories
@EntityScan(basePackages = "ru.i_novus.ms.rdm")
@ImportResource("classpath:rdm-context.xml")
@PropertySource("classpath:rdm-rest-default.properties")
public class RdmRestAutoConfiguration {

}
