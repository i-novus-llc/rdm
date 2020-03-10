package ru.inovus.ms.rdm.impl.entity;

import org.springframework.data.domain.Sort;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.async.AsyncOperationStatus;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "async_log_entry", schema = "n2o_rdm_management")
public class AsyncOperationLogEntryEntity {

    public static final Sort.Order DEFAULT_ORDER = Sort.Order.desc("tsStart");

    @Id
    @Column(name = "id", nullable = false)
    private UUID uuid;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "op_enum", nullable = false, updatable = false)
    private AsyncOperation operation;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false, insertable = false)
    private AsyncOperationStatus status;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "error")
    private String error;

    @Column(name = "payload")
    private String payload;

    @Column(name = "stacktrace")
    private String stackTrace;

    @Column(name = "result")
    private String result;

    @Column(name = "start_ts", nullable = false, updatable = false, insertable = false)
    private LocalDateTime tsStart;

    @Column(name = "end_ts", updatable = false, insertable = false)
    private LocalDateTime tsEnd;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public AsyncOperation getOperation() {
        return operation;
    }

    public void setOperation(AsyncOperation operation) {
        this.operation = operation;
    }

    public AsyncOperationStatus getStatus() {
        return status;
    }

    public void setStatus(AsyncOperationStatus status) {
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

    public LocalDateTime getTsStart() {
        return tsStart;
    }

    public void setTsStart(LocalDateTime tsStart) {
        this.tsStart = tsStart;
    }

    public LocalDateTime getTsEnd() {
        return tsEnd;
    }

    public void setTsEnd(LocalDateTime tsEnd) {
        this.tsEnd = tsEnd;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
