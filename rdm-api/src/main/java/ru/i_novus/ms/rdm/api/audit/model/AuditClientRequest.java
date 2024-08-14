package ru.i_novus.ms.rdm.api.audit.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.n2oapp.platform.i18n.UserException;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Getter
@ToString
public class AuditClientRequest {

    @Setter
    private LocalDateTime eventDate;

    @Setter
    private Short auditType;

    private String context;
    private AuditClientRequestParam eventType;
    private AuditClientRequestParam objectType;
    private AuditClientRequestParam objectId;
    private AuditClientRequestParam objectName;
    private AuditClientRequestParam userId;
    private AuditClientRequestParam username;
    private AuditClientRequestParam sourceWorkstation;
    private AuditClientRequestParam sourceApplication;
    private AuditClientRequestParam hostname;
    private AuditClientRequestParam sender;
    private AuditClientRequestParam receiver;

    public void setEventType(String eventType, Object... args) {
        this.eventType = new AuditClientRequestParam(eventType, args);
    }

    public void setObjectType(String objectType, Object... args) {
        this.objectType = new AuditClientRequestParam(objectType, args);
    }

    public void setObjectId(String objectId, Object... args) {
        this.objectId = new AuditClientRequestParam(objectId, args);
    }

    public void setObjectName(String objectName, Object... args) {
        this.objectName = new AuditClientRequestParam(objectName, args);
    }

    public void setUserId(String userId, Object... args) {
        this.userId = new AuditClientRequestParam(userId, args);
    }

    public void setUsername(String username, Object... args) {
        this.username = new AuditClientRequestParam(username, args);
    }

    public void setSourceWorkstation(String sourceWorkstation, Object... args) {
        this.sourceWorkstation = new AuditClientRequestParam(sourceWorkstation, args);
    }

    public void setSourceApplication(String sourceApplication, Object... args) {
        this.sourceApplication = new AuditClientRequestParam(sourceApplication, args);
    }

    public void setHostname(String hostname, Object... args) {
        this.hostname = new AuditClientRequestParam(hostname, args);
    }

    public void setSender(String sender, Object... args) {
        this.sender = new AuditClientRequestParam(sender, args);
    }

    public void setReceiver(String receiver, Object... args) {
        this.receiver = new AuditClientRequestParam(receiver, args);
    }

    public void setContext(Object contextJson) {
        if (contextJson instanceof String) {
            this.context = (String) contextJson;
        } else if (contextJson == null) {
            this.context = null;
        } else {
            try {
                this.context = new ObjectMapper().writeValueAsString(contextJson);
            } catch (JsonProcessingException e) {
                log.error("Error on parsing context json", e);
                throw new UserException("audit.clientException.invalidContext");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof AuditClientRequest))
            return false;

        AuditClientRequest that = (AuditClientRequest) o;

        if (!Objects.equals(getEventDate(), that.getEventDate())) {
            return false;
        }

        if (!Objects.equals(getAuditType(), that.getAuditType())) {
            return false;
        }

        if (!Objects.equals(getContext(), that.getContext())) {
            return false;
        }

        if (!Objects.equals(getEventType(), that.getEventType())) {
            return false;
        }

        if (!Objects.equals(getObjectType(), that.getObjectType())) {
            return false;
        }

        if (!Objects.equals(getObjectId(), that.getObjectId())) {
            return false;
        }

        if (!Objects.equals(getObjectName(), that.getObjectName())) {
            return false;
        }

        if (!Objects.equals(getUserId(), that.getUserId())) {
            return false;
        }

        if (!Objects.equals(getUsername(), that.getUsername())) {
            return false;
        }

        if (!Objects.equals(getSourceWorkstation(), that.getSourceWorkstation())) {
            return false;
        }

        if (!Objects.equals(getSourceApplication(), that.getSourceApplication())) {
            return false;
        }

        if (!Objects.equals(getHostname(), that.getHostname())) {
            return false;
        }

        if (!Objects.equals(getSender(), that.getSender())) {
            return false;
        }

        return Objects.equals(getReceiver(), that.getReceiver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEventDate(), getAuditType(), getContext(), getEventType(), getObjectType(),
                getObjectId(), getObjectName(), getUserId(), getUsername(), getSourceWorkstation(),
                getSourceApplication(), getHostname(), getSender(), getReceiver());
    }
}