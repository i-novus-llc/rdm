package ru.inovus.ms.rdm.n2o.audit;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.processing.N2oModule;
import net.n2oapp.framework.api.ui.ActionRequestInfo;
import net.n2oapp.framework.api.ui.ActionResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.n2o.audit.creator.AuditLogCreator;
import ru.inovus.ms.rdm.n2o.service.api.AuditLogService;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class N2oAuditModule extends N2oModule {

    @Resource(name = "auditLogCreators")
    Map<String, AuditLogCreator> auditLogCreators;

    @Autowired
    AuditLogService auditLogService;

    @Override
    public void processActionResult(ActionRequestInfo requestInfo, ActionResponseInfo responseInfo, DataSet dataSet) {
        AuditLogCreator auditLogCreator = auditLogCreators.get(
                getActionFullId(requestInfo.getObject().getId(), requestInfo.getOperation().getId())
        );
        if (auditLogCreator == null)
            return;

        auditLogService.addAction(auditLogCreator.create(requestInfo, dataSet));
    }

    private String getActionFullId(String objectId, String actionId) {
        return objectId + "." + actionId;
    }

}
