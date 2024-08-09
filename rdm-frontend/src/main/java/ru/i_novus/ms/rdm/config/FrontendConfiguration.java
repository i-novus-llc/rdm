package ru.i_novus.ms.rdm.config;

import net.n2oapp.framework.config.register.scanner.XmlInfoScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.audit.client.UserAccessor;

import static ru.i_novus.ms.rdm.config.SecurityContextUtils.DEFAULT_USER_ID;
import static ru.i_novus.ms.rdm.config.SecurityContextUtils.DEFAULT_USER_NAME;

@Configuration
public class FrontendConfiguration {

    @Bean
    public XmlInfoScanner myInfoScanner() {
        return new XmlInfoScanner("classpath*:/access/**/*.xml");
    }

    @Bean
    public UserAccessor userAccessor() {
        return this::createUserAccessor;
    }

    private ru.i_novus.ms.audit.client.model.User createUserAccessor() {

        //final Object principal = SecurityContextUtils.getPrincipal();
        //if (principal == null)
            return createAuditUser(DEFAULT_USER_ID, DEFAULT_USER_NAME);

        //if (principal instanceof UserInfoModel) {
        //
        //    final UserInfoModel user = (UserInfoModel) principal;
        //    return createAuditUser(user.email, user.username);
        //
        //} else {
        //    return createAuditUser("" + principal, DEFAULT_USER_NAME);
        //}
    }

    private ru.i_novus.ms.audit.client.model.User createAuditUser(String id, String name) {
        return new ru.i_novus.ms.audit.client.model.User(id != null ? id : name, name);
    }
}
