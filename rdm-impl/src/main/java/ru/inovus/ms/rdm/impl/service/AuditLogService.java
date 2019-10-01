package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.security.admin.api.criteria.UserCriteria;
import net.n2oapp.security.admin.api.model.User;
import net.n2oapp.security.admin.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
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
import static org.apache.commons.text.StringEscapeUtils.escapeJson;

@Service
public class AuditLogService {

    private AuditClient auditClient;

    @Autowired
    @Qualifier("simpleAuditClient")
    public void setAuditClient(AuditClient auditClient) {
        this.auditClient = auditClient;
    }

    @Autowired
    private UserService userService;

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
        OAuth2Authentication auth = ((OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication());
        String username = (String) auth.getPrincipal();
        UserCriteria uc = new UserCriteria();
        uc.setUsername(username);
        Page<User> p = userService.findAll(uc);
        if (p.getTotalElements() != 1)
            throw new RuntimeException("Exactly one user with the name \"" + username + "\" was expected.");
        String userId = p.get().findAny().get().getId().toString();
        request.setEventDate(LocalDateTime.now(Clock.systemUTC()));
        request.setObjectType(action.getObjType());
        request.setObjectName(action.getObjName());
        request.setObjectId(action.getObjId(obj));
        request.setUserId(username);
        request.setUsername(userId);
        request.setEventType(action.getName());
        Map<String, Object> m = new HashMap<>(action.getContext(obj));
        m.putAll(additionalContext);
        request.setContext(toJson(m));
        request.setAuditType((short) 1);
        auditClient.add(request);
    }

    private static String toJson(Map<String, Object> ctx) {
        String s = "{" + ctx.entrySet().stream().map(
                e -> "\"" + escapeJson(e.getKey()) + "\": " + (isStringLiteral(e.getValue()) ? "\"" + escapeJson(e.getValue().toString()) + "\"" : e.getValue())
        ).collect(joining(", ")) +
                '}';
        return s;
    }

    private static boolean isStringLiteral(Object obj) {
        return obj.getClass() == String.class;
    }

}
