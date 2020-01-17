package ru.inovus.ms.rdm.api.async;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AsyncOperationLogEntry {

    private UUID uuid;
    private AsyncOperation operation;
    private AsyncOperation.Status status;
    private String error;
    private Map<String, Object> payload;
    private Object result;
    private LocalDateTime tsStartUTC;
    private LocalDateTime tsEndUTC;

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

    public AsyncOperation.Status getStatus() {
        return status;
    }

    public void setStatus(AsyncOperation.Status status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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
