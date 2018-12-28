package ru.inovus.ms.rdm.audit;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.exception.N2oException;
import net.n2oapp.framework.api.processing.N2oModule;
import net.n2oapp.framework.api.ui.ActionRequestInfo;
import net.n2oapp.framework.api.ui.ActionResponseInfo;
import net.n2oapp.framework.engine.rest.N2oRestBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.audit.creator.AuditLogCreator;
import ru.inovus.ms.rdm.model.audit.AuditAction;
import ru.inovus.ms.rdm.model.audit.AuditLog;
import ru.inovus.ms.rdm.service.api.AuditLogService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class N2oAuditModule extends N2oModule {

    private static final Logger logger = LoggerFactory.getLogger(N2oAuditModule.class);

    @Autowired
    Map<String, AuditLogCreator> auditLogCreators;

    @Autowired
    AuditLogService auditLogService;

    @Override
    public void processActionError(ActionRequestInfo<DataSet> requestInfo, ActionResponseInfo responseInfo, DataSet dataSet, N2oException exception) {
        AuditLog actionEntity = getAuditLog(requestInfo, dataSet, exception);
        if (actionEntity != null)
            System.out.println(actionEntity.getUser() + ". " + actionEntity.getAction().getName());
//            auditLogService.addAction(actionEntity);
        else System.out.println("empty action");
    }

    @Override
    public void processActionResult(ActionRequestInfo requestInfo, ActionResponseInfo responseInfo, DataSet dataSet) {
        AuditLog actionEntity =
                auditLogCreators
                        .get(getActionFullId(requestInfo.getObject().getName(),
                                requestInfo.getOperation().getId()))
                        .create(requestInfo, dataSet);
//            auditLogService.addAction(actionEntity);
        if (actionEntity != null)
            System.out.println(actionEntity.getUser() + ". " + requestInfo.getOperation().getId());
        else System.out.println("empty action");
    }

    private AuditLog getAuditLog(ActionRequestInfo<DataSet> requestInfo, DataSet dataSet, N2oException exception) {
        Map<String, Object> dataMap = new HashMap<>();
        dataSet.keySet().forEach(key -> dataMap.put(key, dataSet.get(key)));
        if (exception != null) {
            if (exception instanceof N2oRestBusinessException)
                dataMap.put("errorMessage", ((N2oRestBusinessException) exception).getSummary());
            else
                return null;
        }

        logger.info("{} - {}", requestInfo.getUser().getUsername(),
                getActionFullId(requestInfo.getObject().getName(), requestInfo.getOperation().getId()));

        return new AuditLog(null, requestInfo.getUser().getUsername(), LocalDateTime.now(),
                AuditAction.UPLOAD, dataSet.get("code") != null ? dataSet.get("code").toString() : null);
    }

    private String getActionFullId(String objectId, String actionId) {
        return objectId + "." + actionId;
    }

}
