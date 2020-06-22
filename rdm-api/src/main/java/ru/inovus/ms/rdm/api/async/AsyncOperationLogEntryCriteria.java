package ru.inovus.ms.rdm.api.async;

import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.UUID;

public class AsyncOperationLogEntryCriteria extends AbstractCriteria {

    @QueryParam("uuid")
    private UUID id;

    @QueryParam("operation")
    private AsyncOperation operation;

    @QueryParam("status")
    private AsyncOperationStatus status;

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
}
