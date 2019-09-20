package ru.inovus.ms.rdm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.model.audit.AuditAction;
import ru.inovus.ms.rdm.api.model.audit.AuditLog;
import ru.inovus.ms.rdm.api.service.AuditLogService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class RdmAuditedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    AuditLogService auditLogService;

    public RdmAuditedAuthenticationSuccessHandler() {
        setAlwaysUseDefaultTargetUrl(true);
    }

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
