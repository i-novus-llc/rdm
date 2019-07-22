package ru.inovus.ms.rdm.entity;

import ru.inovus.ms.rdm.model.audit.AuditAction;
import ru.inovus.ms.rdm.util.TimeUtils;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", schema = "audit")
public class AuditLogEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String user;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "action", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private AuditAction action;

    @Column(name = "context")
    private String context;

    public AuditLogEntity() {
    }

    public AuditLogEntity(String user, LocalDateTime date, AuditAction action, String context) {
        this.user = user;
        this.date = date;
        this.action = action;
        this.context = context;
    }

    public AuditLogEntity(String user, AuditAction action, String context) {
        this.user = user;
        this.action = action;
        this.context = context;
    }

    @PrePersist
    public void prePersist() {
        if (date == null)
            date = TimeUtils.now();
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
}
