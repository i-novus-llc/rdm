package ru.inovus.ms.rdm;

import net.n2oapp.framework.config.register.scanner.XmlInfoScanner;
import net.n2oapp.security.admin.rest.client.AdminRestClientConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import ru.inovus.ms.rdm.api.util.RdmPermission;
import ru.inovus.ms.rdm.handler.RdmExceptionHandler;
import ru.inovus.ms.rdm.n2o.RdmWebConfiguration;
import ru.inovus.ms.rdm.util.RdmPermissionImpl;

@SpringBootApplication (scanBasePackageClasses = { FrontendApplication.class, AdminRestClientConfiguration.class })
@Import({ RdmWebConfiguration.class })
public class FrontendApplication {

    @Bean
    public XmlInfoScanner myInfoScanner() {
        return new XmlInfoScanner("classpath*:/access/**/*.xml");
    }

    @Bean
    @Primary
    public RdmExceptionHandler exceptionHandler() {
        return new RdmExceptionHandler();
    }

    @Bean
    @Primary
    public RdmPermission rdmPermission() {
        return new RdmPermissionImpl();
    }


    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
