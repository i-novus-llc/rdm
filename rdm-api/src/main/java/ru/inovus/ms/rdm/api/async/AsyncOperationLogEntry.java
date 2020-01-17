package ru.inovus.ms.rdm.api.async;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AsyncOperationLogEntry {

    private UUID uuid;
    private AsyncOperation operation;
    private AsyncOperationStatus status;
    private String error;
    private Map<String, Object> payload;
    private Object result;
    private LocalDateTime tsStart;
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

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

}
