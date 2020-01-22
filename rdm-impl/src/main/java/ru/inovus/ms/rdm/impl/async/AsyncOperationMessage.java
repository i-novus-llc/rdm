package ru.inovus.ms.rdm.impl.async;

import ru.i_novus.ms.audit.client.model.User;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class AsyncOperationMessage implements Serializable {

    public static final String ARGS_KEY = "args";
    public static final String USER_KEY = "user";

    private Object[] args; // NOSONAR
    private String userName;
    private UUID operationId;
    private AsyncOperation operation;

    public AsyncOperationMessage(Object[] args, User user, UUID operationId, AsyncOperation operation) {
        this.args = args == null ? new Object[0] : args;
        this.userName = user.getUsername();
        this.operationId = operationId;
        this.operation = operation;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public AsyncOperation getOperation() {
        return operation;
    }

    public void setOperation(AsyncOperation operation) {
        this.operation = operation;
    }

    public String getPayloadAsJson() {
        return JsonUtil.getAsJson(Map.of(ARGS_KEY, args, USER_KEY, userName));
    }

    @Override
    public String toString() {
        return "AsyncOperationMessage{" +
                "args=" + Arrays.toString(args) +
                ", userName='" + userName + '\'' +
                ", operationId=" + operationId +
                ", operation=" + operation +
                '}';
    }

}
