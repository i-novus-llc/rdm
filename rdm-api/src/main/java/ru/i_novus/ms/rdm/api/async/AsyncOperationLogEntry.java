package ru.i_novus.ms.rdm.api.async;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.util.UUID;

@ApiModel("Асинхронная операция")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsyncOperationLogEntry {

    @ApiModelProperty("Идентификатор операции")
    private UUID id;

    @ApiModelProperty("Тип операции")
    private AsyncOperationTypeEnum operationType;

    @ApiModelProperty("Код справочника")
    private String code;

    @ApiModelProperty("Статус операции")
    private AsyncOperationStatusEnum status;

    @ApiModelProperty("Дата создания операции")
    private LocalDateTime tsStart;

    @ApiModelProperty("Дата окончания обработки операции")
    private LocalDateTime tsEnd;

    @ApiModelProperty("Полезная нагрузка в формате JSON")
    private String payload;

    @ApiModelProperty("Результат операции в формате JSON")
    private String result;

    @ApiModelProperty("Текст ошибки операции")
    private String error;

    @ApiModelProperty("Трассировка стека в случае ошибки")
    private String stackTrace;

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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public String toString() {
        return "AsyncOperationLogEntry{" +
                "id=" + id +
                ", operationType=" + operationType +
                ", code='" + code + '\'' +
                ", status=" + status +
                ", tsStart=" + tsStart +
                ", tsEnd=" + tsEnd +
                ", payload='" + payload + '\'' +
                ", result='" + result + '\'' +
                ", error='" + error + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                '}';
    }
}
