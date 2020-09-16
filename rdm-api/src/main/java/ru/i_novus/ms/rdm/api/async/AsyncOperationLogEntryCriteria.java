package ru.i_novus.ms.rdm.api.async;

import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.UUID;

/**
 * Асинхронная операция: Критерий поиска.
 */
public class AsyncOperationLogEntryCriteria extends AbstractCriteria {

    public static final Sort.Order DEFAULT_ORDER = Sort.Order.desc("tsStart");

    @QueryParam("id")
    private UUID id;

    @QueryParam("operationType")
    private AsyncOperationTypeEnum operationType;

    @QueryParam("code")
    private String code;

    @QueryParam("status")
    private AsyncOperationStatusEnum status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    @Override
    protected List<Sort.Order> getDefaultOrders() {
        return List.of(DEFAULT_ORDER);
    }
}
