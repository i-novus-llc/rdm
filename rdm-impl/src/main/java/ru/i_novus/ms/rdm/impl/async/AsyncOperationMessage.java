package ru.i_novus.ms.rdm.impl.async;

import ru.i_novus.ms.audit.client.model.User;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Асинхронная операция: Сообщение.
 */
class AsyncOperationMessage implements Serializable {

    public static final String ARGS_KEY = "args";
    public static final String USER_KEY = "user";

    /**
     * Идентификатор операции.
     */
    private UUID operationId;

    /**
     * Тип операции.
     */
    private AsyncOperationTypeEnum operationType;

    /**
     * Код справочника.
     */
    private String code;

    /**
     * Аргументы операции.
     */
    private Serializable[] args;

    /**
     * Имя пользователя.
     */
    private String userName;

    public AsyncOperationMessage(UUID operationId, AsyncOperationTypeEnum operationType,
                                 String code, Serializable[] args, User user) {

        this.operationId = operationId;
        this.operationType = operationType;

        this.code = code;
        this.args = args == null ? new Serializable[0] : args;
        this.userName = user.getUsername();
    }

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
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

    public Serializable[] getArgs() {
        return args;
    }

    public void setArgs(Serializable[] args) {
        this.args = args;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPayloadAsJson() {
        return JsonUtil.getAsJson(Map.of(ARGS_KEY, args, USER_KEY, userName));
    }

    @Override
    public String toString() {
        return "AsyncOperationMessage{" +
                "operationId=" + operationId +
                ", operationType=" + operationType +
                ", code='" + code + '\'' +
                ", args=" + Arrays.toString(args) +
                ", userName='" + userName + '\'' +
                '}';
    }
}
