package ru.inovus.ms.rdm.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.audit.client.AuditClient;
import ru.i_novus.ms.audit.client.model.AuditClientRequest;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.impl.audit.AuditAction;

import java.time.Clock;
import java.time.LocalDateTime;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                throw new IllegalArgumentException("Some of the disabled actions are not mentioned in ru.inovus.ms.rdm.impl.audit.AuditAction enum.");
            List<AuditAction> list = disabled.stream().map(s -> AuditAction.valueOf(values.stream().filter(s::equalsIgnoreCase).findFirst().get())).collect(toList());
            disabledActions = list.isEmpty() ? EnumSet.noneOf(AuditAction.class) : EnumSet.copyOf(list);
        }
    }

    @Transactional
    public void addAction(AuditAction action, Supplier getObjectFunction) {
        addAction(action, getObjectFunction, emptyMap());
    }

    @Transactional
    public void addAction(AuditAction action, Supplier getObjectFunction, Map<String, Object> additionalContext) {
        if (!disabledActions.contains(action)) {
            Object obj = getObjectFunction.get();
            AuditClientRequest request = new AuditClientRequest();
            request.setEventDate(LocalDateTime.now(Clock.systemUTC()));
            request.setObjectType(action.getObjType());
            request.setObjectName(action.getObjName());
            request.setObjectId(action.getObjId(obj));
            request.setEventType(action.getName());
            Map<String, Object> m = new HashMap<>(action.getContext(obj));
            m.putAll(additionalContext);
            request.setContext(toJson(m));
            request.setAuditType((short) 1);
            auditClient.add(request);
        } else {
            logger.warn("audit action {} disabled", action);
        }
    }

    private static String toJson(Map<String, Object> ctx) {
        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(ctx);
        } catch (JsonProcessingException e) {
            /*Не выбросится*/
            logger.error("Error while serializing to json {0}", e);
            throw new RdmException(e);
        }
        return json;
    }

}
