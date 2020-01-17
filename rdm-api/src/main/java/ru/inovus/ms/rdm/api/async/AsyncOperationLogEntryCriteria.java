package ru.inovus.ms.rdm.api.async;

import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.UUID;

public class AsyncOperationLogEntryCriteria extends AbstractCriteria {

    @QueryParam("status")
    private AsyncOperationStatus status;

    @QueryParam("opType")
    private AsyncOperation operation;

    @QueryParam("opId")
    private UUID uuid;

    public AsyncOperationStatus getStatus() {
        return status;
    }

    public void setStatus(AsyncOperationStatus status) {
        this.status = status;
    }

    public AsyncOperation getOperation() {
        return operation;
    }

    public void setOperation(AsyncOperation operation) {
        this.operation = operation;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
