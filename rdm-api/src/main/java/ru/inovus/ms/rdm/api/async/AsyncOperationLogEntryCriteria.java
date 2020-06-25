package ru.inovus.ms.rdm.api.async;

import org.springframework.data.domain.Sort;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.UUID;

public class AsyncOperationLogEntryCriteria extends AbstractCriteria {

    public static final Sort.Order DEFAULT_ORDER = Sort.Order.desc("tsStart");

    @QueryParam("status")
    private AsyncOperationStatus status;

    @QueryParam("operation")
    private AsyncOperation operation;

    @QueryParam("uuid")
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

    @Override
    protected List<Sort.Order> getDefaultOrders() {
        return List.of(DEFAULT_ORDER);
    }
}
