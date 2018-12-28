package ru.inovus.ms.rdm.audit.creator;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.ui.ActionRequestInfo;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.model.audit.AuditAction;
import ru.inovus.ms.rdm.model.audit.AuditLog;

import java.time.LocalDateTime;

@Component
public class PublicationAuditLogCreator implements AuditLogCreator {

    private static final AuditAction action = AuditAction.PUBLICATION;

    @Override
    public AuditLog create(ActionRequestInfo requestInfo, DataSet dataSet) {

        return new AuditLog(null, requestInfo.getUser().getUsername(), LocalDateTime.now(), action,
                dataSet.get("code") != null ? dataSet.get("code").toString() : null);
    }

}
