package ru.i_novus.ms.rdm;

import net.n2oapp.security.admin.rest.client.AdminRestClientConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import ru.i_novus.ms.rdm.config.SecurityConfig;
import ru.i_novus.ms.rdm.n2o.config.RdmWebConfiguration;

@SpringBootApplication(scanBasePackageClasses = {
        FrontendApplication.class,
        AdminRestClientConfiguration.class
})
@Import({
        RdmWebConfiguration.class,
        SecurityConfig.class
})
@ComponentScan(basePackages = "ru.i_novus.ms.rdm.config")
public class FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
