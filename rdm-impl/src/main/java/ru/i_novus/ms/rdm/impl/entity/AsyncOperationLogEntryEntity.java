package ru.i_novus.ms.rdm.impl.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.i_novus.ms.rdm.api.async.AsyncOperationStatusEnum;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import static ru.i_novus.ms.rdm.api.util.json.JsonUtil.toJsonString;

/**
 * Асинхронная операция: Сущность.
 */
@Entity
@Table(name = "async_log_entry", schema = "n2o_rdm_management")
public class AsyncOperationLogEntryEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    private UUID uuid;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "op_enum", nullable = false, updatable = false)
    private AsyncOperationTypeEnum operationType;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false, insertable = false)
    private AsyncOperationStatusEnum status;

    @Column(name = "start_ts", nullable = false, updatable = false, insertable = false)
    private LocalDateTime tsStart;

    @Column(name = "end_ts", updatable = false, insertable = false)
    private LocalDateTime tsEnd;

    @Column(name = "payload")
    private String payload;

    @Column(name = "result")
    private String result;

    @Column(name = "error")
    private String error;

    @Column(name = "stacktrace")
    private String stackTrace;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public AsyncOperationTypeEnum getOperationType() {
        return operationType;
    }

    public void setOperationType(AsyncOperationTypeEnum operationType) {
        this.operationType = operationType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public AsyncOperationStatusEnum getStatus() {
        return status;
    }

    public void setStatus(AsyncOperationStatusEnum status) {
        this.status = status;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @JsonIgnore
    public void setSerializableResult(Serializable result) {

        String jsonResult = (result == null) ? null : toJsonString(result);
        setResult(jsonResult);
    }
}
