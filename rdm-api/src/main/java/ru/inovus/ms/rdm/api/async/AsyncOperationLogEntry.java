package ru.inovus.ms.rdm.api.async;

import java.time.LocalDateTime;
import java.util.UUID;

public class AsyncOperationLogEntry {

    private UUID id;
    private String code;
    private AsyncOperation operation;
    private AsyncOperationStatus status;
    private String error;
    private String payload;
    private String result;
    private LocalDateTime tsStart;
    private LocalDateTime tsEnd;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "AsyncOperationLogEntry{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", operation=" + operation +
                ", status=" + status +
                ", error='" + error + '\'' +
                ", payload=" + payload +
                ", result=" + result +
                ", tsStart=" + tsStart +
                ", tsEnd=" + tsEnd +
                '}';
    }

}
