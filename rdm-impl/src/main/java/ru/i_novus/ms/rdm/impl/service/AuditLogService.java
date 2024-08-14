package ru.i_novus.ms.rdm.impl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.audit.AuditClient;
import ru.i_novus.ms.rdm.api.audit.model.AuditClientRequest;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private EnumSet<AuditAction> disabledActions;

    private AuditClient auditClient;

    @Autowired
    public void setAuditClient(AuditClient auditClient) {
        this.auditClient = auditClient;
    }

    @Value("${rdm.audit.disabledActions:}#{T(java.util.Collections).emptyList()}")
    public void setDisabled(List<String> disabled) {
        if (disabled.stream().anyMatch("all"::equalsIgnoreCase))
            disabledActions = EnumSet.allOf(AuditAction.class);
        else {
            List<String> values = stream(AuditAction.values()).map(Enum::name).collect(toList());
            if (disabled.stream().anyMatch(s -> values.stream().noneMatch(s::equalsIgnoreCase)))
                throw new IllegalArgumentException("Some of the disabled actions are not mentioned in ru.i_novus.ms.rdm.impl.audit.AuditAction enum.");
            List<AuditAction> list = disabled.stream().map(s -> AuditAction.valueOf(values.stream().filter(s::equalsIgnoreCase).findFirst().get())).collect(toList());
            disabledActions = list.isEmpty() ? EnumSet.noneOf(AuditAction.class) : EnumSet.copyOf(list);
        }
    }

    @Transactional
    public void addAction(AuditAction action, Supplier<Object> getObjectFunction) {
        addAction(action, getObjectFunction, emptyMap());
    }

    @Transactional
    public void addAction(AuditAction action, Supplier<Object> getObjectFunction,
                          Map<String, Object> additionalContext) {

        if (!disabledActions.contains(action)) {
            logger.info("audit action:\n{}", action);
            logger.info("audit context:\n{}", additionalContext);

            final Object obj = getObjectFunction.get();
            logger.info("audit object:\n{}", obj);

            final AuditClientRequest request = new AuditClientRequest();
            request.setObjectType(action.getObjType());
            request.setObjectName(action.getObjName());
            request.setObjectId(action.getObjId(obj));
            request.setEventType(action.getName());
            Map<String, Object> contextMap = new HashMap<>(action.getContext(obj));
            contextMap.putAll(additionalContext);
            request.setContext(JsonUtil.toJsonString(contextMap));
            request.setAuditType((short) 1);
            try {
                auditClient.add(request);
            } catch (Exception e) {
                logger.error("An error occurred during the audit.", e);
            }
        } else {
            logger.warn("audit action {} disabled", action);
        }
    }

}
