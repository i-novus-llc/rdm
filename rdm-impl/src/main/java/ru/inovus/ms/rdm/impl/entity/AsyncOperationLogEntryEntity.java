package ru.inovus.ms.rdm.impl.entity;

import ru.inovus.ms.rdm.api.async.Async;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "async_log_entry", schema = "n2o_rdm_management")
public class AsyncOperationLogEntryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID uuid;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "op_enum", nullable = false, updatable = false)
    private Async.Operation operation;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false, insertable = false)
    private Async.Operation.Status status;

    @Column(name = "error")
    private String error;

    @Column(name = "payload")
    private String payload;

    @Column(name = "result")
    private String result;

    @Column(name = "start_ts", nullable = false, updatable = false, insertable = false)
    private LocalDateTime tsStartUTC;

    @Column(name = "end_ts", updatable = false, insertable = false)
    private LocalDateTime tsEndUTC;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Async.Operation getOperation() {
        return operation;
    }

    public void setOperation(Async.Operation operation) {
        this.operation = operation;
    }

    public Async.Operation.Status getStatus() {
        return status;
    }

    public void setStatus(Async.Operation.Status status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getTsStartUTC() {
        return tsStartUTC;
    }

    public void setTsStartUTC(LocalDateTime tsStartUTC) {
        this.tsStartUTC = tsStartUTC;
    }

    public LocalDateTime getTsEndUTC() {
        return tsEndUTC;
    }

    public void setTsEndUTC(LocalDateTime tsEndUTC) {
        this.tsEndUTC = tsEndUTC;
    }

}
