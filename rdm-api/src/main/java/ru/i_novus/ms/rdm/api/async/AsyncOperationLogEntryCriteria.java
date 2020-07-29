package ru.i_novus.ms.rdm.api.async;

import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.UUID;

public class AsyncOperationLogEntryCriteria extends AbstractCriteria {

    public static final Sort.Order DEFAULT_ORDER = Sort.Order.desc("tsStart");

    @QueryParam("id")
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

    @Override
    protected List<Sort.Order> getDefaultOrders() {
        return List.of(DEFAULT_ORDER);
    }
}
