package ru.inovus.ms.rdm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.audit.creator.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuditConfiguration {

    @Autowired
    PublicationAuditLogCreator publicationAuditLogCreator;

    @Autowired
    CreateRefBookAuditLogCreator createRefBookAuditLogCreator;

    @Autowired
    DeleteRefBookAuditLogCreator deleteRefBookAuditLogCreator;

    @Autowired
    UploadAuditLogCreator uploadAuditLogCreator;

    @Bean
    public Map<String, AuditLogCreator> auditLogCreators() {
        Map<String, AuditLogCreator> auditLogCreators = new HashMap<>();
        auditLogCreators.put("refBook.publish", publicationAuditLogCreator);
        auditLogCreators.put("refBook.create", createRefBookAuditLogCreator);
        auditLogCreators.put("refBook.delete", deleteRefBookAuditLogCreator);
        auditLogCreators.put("refBook.upload", uploadAuditLogCreator);
        return auditLogCreators;
    }

}
