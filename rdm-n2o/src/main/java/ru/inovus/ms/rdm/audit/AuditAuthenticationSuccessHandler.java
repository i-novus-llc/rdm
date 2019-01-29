package ru.inovus.ms.rdm.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.model.audit.AuditAction;
import ru.inovus.ms.rdm.model.audit.AuditLog;
import ru.inovus.ms.rdm.service.api.AuditLogService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class AuditAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    AuditLogService auditLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        AuditLog auditLog =
                new AuditLog(null, authentication.getName(), LocalDateTime.now(), AuditAction.LOGIN, null);
        auditLogService.addAction(auditLog);
        super.onAuthenticationSuccess(request, response, authentication);
    }

}