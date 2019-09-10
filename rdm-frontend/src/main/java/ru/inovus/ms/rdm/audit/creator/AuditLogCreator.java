package ru.inovus.ms.rdm.audit.creator;

import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.ui.ActionRequestInfo;
import ru.inovus.ms.rdm.model.audit.AuditLog;

public interface AuditLogCreator {

    AuditLog create(ActionRequestInfo requestInfo, DataSet dataSet);

}
