package ru.inovus.ms.rdm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.n2o.model.audit.AuditAction;
import ru.inovus.ms.rdm.n2o.model.audit.AuditLog;
import ru.inovus.ms.rdm.n2o.service.api.AuditLogService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Component
public class AuditLogoutHandler implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogoutHandler.class);

    @Autowired
    AuditLogService auditLogService;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        if (response.getStatus() == 200) {
            AuditLog auditLog =
                    new AuditLog(null, authentication.getName(), LocalDateTime.now(), AuditAction.LOGOUT, null);
            auditLogService.addAction(auditLog);
        } else {
            logger.warn("{}: LOGOUT audit log ignored", response.getStatus());
        }
    }

}
