package ru.inovus.ms.rdm.n2o.audit.creator;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.ui.ActionRequestInfo;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.model.audit.AuditAction;
import ru.inovus.ms.rdm.api.model.audit.AuditLog;

import java.time.LocalDateTime;

@Component
public class CreateRefBookAuditLogCreator implements AuditLogCreator {

    private static final AuditAction auditAction = AuditAction.CREATE_REF_BOOK;

    @Override
    public AuditLog create(ActionRequestInfo requestInfo, DataSet dataSet) {

        return new AuditLog(null,
                requestInfo.getUser().getUsername(),
                LocalDateTime.now(),
                auditAction,
                dataSet.get("code").toString());
    }

}
