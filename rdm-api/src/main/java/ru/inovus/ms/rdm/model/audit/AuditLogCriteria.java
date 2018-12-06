package ru.inovus.ms.rdm.model.audit;

import net.n2oapp.platform.jaxrs.RestCriteria;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;

public class AuditLogCriteria extends RestCriteria {

    @QueryParam("user")
    private String user;
    @QueryParam("action")
    private AuditAction action;
    @QueryParam("fromDate")
    private LocalDateTime fromDate;
    @QueryParam("toDate")
    private LocalDateTime toDate;
    @QueryParam("context")
    private String context;

    public AuditLogCriteria() {
        setPageSize(10);
    }

    public AuditLogCriteria(String user, AuditAction action, LocalDateTime fromDate, LocalDateTime toDate, String context) {
        this();
        this.user = user;
        this.action = action;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.context = context;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
