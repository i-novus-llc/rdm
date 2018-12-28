package ru.inovus.ms.rdm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.audit.creator.AuditLogCreator;
import ru.inovus.ms.rdm.audit.creator.PublicationAuditLogCreator;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuditConfiguration {

    @Autowired
    PublicationAuditLogCreator publicationAuditLogCreator;

    @Autowired
    AuditLogCreator editAuditLogCreator;

    @Bean
    public Map<String, AuditLogCreator> auditLogCreators() {
        return new HashMap<String, AuditLogCreator>(){{
            put("refBook.publish", publicationAuditLogCreator);
            put("refBook.edit", editAuditLogCreator);
        }};
    }

}
