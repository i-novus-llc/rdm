package ru.inovus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.audit.client.AuditClient;
import ru.i_novus.ms.audit.client.model.AuditClientRequest;
import ru.inovus.ms.rdm.api.model.audit.AuditAction;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

@Service
public class AuditLogService {

    @Autowired
    private AuditClient auditClient;

    private EnumSet<AuditAction> disabledActions;

    @Value("${rdm.audit.disabledActions}")
    public void setDisabled(List<String> disabled) {
        if (disabled.stream().anyMatch(s -> s.toLowerCase().equals("all")))
            disabledActions = EnumSet.allOf(AuditAction.class);
        else {
            List<String> values = stream(AuditAction.values()).map(Enum::name).collect(toList());
            if (disabled.stream().anyMatch(s -> values.stream().noneMatch(s::equalsIgnoreCase)))
                throw new IllegalArgumentException("Some of the disabled actions are not mentioned in ru.inovus.ms.rdm.api.model.audit.AuditAction enum.");
            List<AuditAction> list = disabled.stream().map(s -> AuditAction.valueOf(values.stream().filter(s::equalsIgnoreCase).findFirst().get())).collect(toList());
            disabledActions = list.size() == 0 ? EnumSet.noneOf(AuditAction.class) : EnumSet.copyOf(list);
        }
    }

    @Autowired
    @SuppressWarnings("unused")
    public AuditLogService() {}

    public void addAction(AuditAction action, LocalDateTime date, Map<String, String> ctx) {
        if (!disabledActions.contains(action))
            audit(action, date, ctx);
    }

    private void audit(AuditAction action, LocalDateTime date, Map<String, String> ctx) {
        AuditClientRequest request = new AuditClientRequest();
        request.setEventDate(date);
        request.setObjectType(action.getObjType());
        request.setObjectName(action.getObjName());
        request.setObjectId(action.getObjId(ctx));
        request.setUserId("1");
        request.setUsername("rdm");
        request.setEventType(action.getName());
        request.setContext(action.toString());
        request.setAuditType((short) 1);
        auditClient.add(request);
    }

}
