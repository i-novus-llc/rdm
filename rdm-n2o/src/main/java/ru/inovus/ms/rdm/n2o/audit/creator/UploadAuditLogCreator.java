package ru.inovus.ms.rdm.n2o.audit.creator;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.ui.ActionRequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.n2o.model.audit.AuditAction;
import ru.inovus.ms.rdm.n2o.model.audit.AuditLog;
import ru.inovus.ms.rdm.n2o.service.api.RefBookService;
import ru.inovus.ms.rdm.n2o.service.api.VersionService;

import java.time.LocalDateTime;

@Component
public class UploadAuditLogCreator implements AuditLogCreator {

    private static final AuditAction auditAction = AuditAction.UPLOAD;

    @Autowired
    VersionService versionService;

    @Autowired
    private RefBookService refBookService;

    @Override
    public AuditLog create(ActionRequestInfo requestInfo, DataSet dataSet) {

        final Integer refBookId = dataSet.getInteger("refBookId");
        String refBookCode = null;
        if (refBookId != null) {
            refBookCode = refBookService.getCode(refBookId);
        } else {
            refBookCode = versionService.getById(dataSet.getInteger("versionId")).getCode();
        }


        return new AuditLog(null,
                requestInfo.getUser().getUsername(),
                LocalDateTime.now(),
                auditAction,
                refBookCode);
    }
}
