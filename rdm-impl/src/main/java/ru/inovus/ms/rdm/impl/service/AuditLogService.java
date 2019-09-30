package ru.inovus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.audit.client.AuditClient;
import ru.i_novus.ms.audit.client.model.AuditClientRequest;
import ru.inovus.ms.rdm.impl.audit.AuditAction;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Service
public class AuditLogService {

    @Autowired
    private AuditClient auditClient;

    private EnumSet<AuditAction> disabledActions;

    @Value("${rdm.audit.disabledActions}")
    public void setDisabled(List<String> disabled) {
        if (disabled.stream().anyMatch("all"::equalsIgnoreCase))
            disabledActions = EnumSet.allOf(AuditAction.class);
        else {
            List<String> values = stream(AuditAction.values()).map(Enum::name).collect(toList());
            if (disabled.stream().anyMatch(s -> values.stream().noneMatch(s::equalsIgnoreCase)))
                throw new IllegalArgumentException("Some of the disabled actions are not mentioned in ru.inovus.ms.rdm.impl.audit.AuditAction enum.");
            List<AuditAction> list = disabled.stream().map(s -> AuditAction.valueOf(values.stream().filter(s::equalsIgnoreCase).findFirst().get())).collect(toList());
            disabledActions = list.isEmpty() ? EnumSet.noneOf(AuditAction.class) : EnumSet.copyOf(list);
        }
    }

    @Autowired
    @SuppressWarnings("unused")
    public AuditLogService() {}

    void addAction(AuditAction action, Object obj) {
        addAction(action, obj, emptyMap());
    }

    void addAction(AuditAction action, Object obj, Map<String, Object> additionalContext) {
        if (!disabledActions.contains(action))
            audit(action, obj, additionalContext);
    }

    private void audit(AuditAction action, Object obj, Map<String, Object> additionalContext) {
        AuditClientRequest request = new AuditClientRequest();
        request.setEventDate(LocalDateTime.now(Clock.systemUTC()));
        request.setObjectType(action.getObjType());
        request.setObjectName(action.getObjName());
        request.setObjectId(action.getObjId(obj));
        request.setUserId("1");
        request.setUsername("rdm");
        request.setEventType(action.getName());
        Map<String, Object> m = new HashMap<>(action.getContext(obj));
        m.putAll(additionalContext);
        request.setContext(toJson(m));
        request.setAuditType((short) 1);
        auditClient.add(request);
    }

    private static String toJson(Map<String, Object> ctx) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(ctx.entrySet().stream().map(
            e -> "\"" + e.getKey() + "\":" + (isStringLiteral(e.getValue()) ? "\"" + e.getValue() + "\"" : e.getValue())
        ).collect(joining(",")));
        return sb.append('}').toString();
    }

    private static boolean isStringLiteral(Object obj) {
        return obj.getClass() == String.class;
    }

}
