package ru.i_novus.ms.rdm;

import net.n2oapp.framework.config.register.scanner.XmlInfoScanner;
import net.n2oapp.security.admin.rest.client.AdminRestClientConfiguration;
import net.n2oapp.security.auth.common.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import ru.i_novus.ms.audit.client.UserAccessor;
import ru.i_novus.ms.rdm.n2o.config.N2oConfiguration;
import ru.i_novus.ms.rdm.n2o.config.RdmWebConfiguration;

import static ru.i_novus.ms.rdm.SecurityContextUtils.DEFAULT_USER_ID;
import static ru.i_novus.ms.rdm.SecurityContextUtils.DEFAULT_USER_NAME;

@SpringBootApplication (scanBasePackageClasses = { FrontendApplication.class, AdminRestClientConfiguration.class })
@Import({ N2oConfiguration.class, RdmWebConfiguration.class })
public class FrontendApplication {

    @Bean
    public XmlInfoScanner myInfoScanner() {
        return new XmlInfoScanner("classpath*:/access/**/*.xml");
    }

    @Bean
    public UserAccessor auditUser() {
        return () -> {
            User user = SecurityContextUtils.getPrincipal();
            return (user == null)
                    ? createAuditUser(DEFAULT_USER_ID, DEFAULT_USER_NAME)
                    : createAuditUser(user.getEmail(), user.getUsername());
        };
    }

    private ru.i_novus.ms.audit.client.model.User createAuditUser(String id, String name) {
        return new ru.i_novus.ms.audit.client.model.User(id != null ? id : name, name);
    }

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
