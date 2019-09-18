package ru.inovus.ms.rdm.n2o;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.n2o.audit.creator.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuditConfiguration {

    @Bean
    public Map<String, AuditLogCreator> auditLogCreators(PublicationAuditLogCreator publicationAuditLogCreator,
                                                         CreateRefBookAuditLogCreator createRefBookAuditLogCreator,
                                                         DeleteRefBookAuditLogCreator deleteRefBookAuditLogCreator,
                                                         UploadAuditLogCreator uploadAuditLogCreator) {
        Map<String, AuditLogCreator> auditLogCreators = new HashMap<>();
        auditLogCreators.put("refBook.publish", publicationAuditLogCreator);
        auditLogCreators.put("refBook.create", createRefBookAuditLogCreator);
        auditLogCreators.put("refBook.delete", deleteRefBookAuditLogCreator);
        auditLogCreators.put("refBook.upload", uploadAuditLogCreator);
        return auditLogCreators;
    }

}
