package ru.inovus.ms.rdm.model.audit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@ApiModel
public class AuditLog {

    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Integer id;
    @ApiModelProperty(required = true)
    private String user;
    private LocalDateTime date;
    @ApiModelProperty(required = true)
    private AuditAction action;
    private String context;

    public AuditLog() {
    }

    public AuditLog(Integer id, String user, LocalDateTime date, AuditAction action, String context) {
        this.id = id;
        this.user = user;
        this.date = date;
        this.action = action;
        this.context = context;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuditLog that = (AuditLog) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (action != that.action) return false;
        return context != null ? context.equals(that.context) : that.context == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", user='" + user + '\'' +
                ", date=" + date +
                ", action=" + action +
                ", context='" + context + '\'' +
                '}';
    }
}
