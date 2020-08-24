package ru.i_novus.ms.rdm.api.model.draft;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

@ApiModel(value = "Модель пост-публикации черновика",
        description = "Набор входных параметров для пост-публикации черновика")
public class PostPublishRequest implements Serializable {

    @ApiModelProperty("Код хранилища ранее опубликованной версии")
    private String lastStorageCode;

    @ApiModelProperty("Код хранилища старого черновика")
    private String oldStorageCode;

    @ApiModelProperty("Код хранилища опубликованного черновика")
    private String newStorageCode;

    @ApiModelProperty("Дата начала действия опубликованной версии")
    private LocalDateTime fromDate;

    @ApiModelProperty("Дата окончания действия опубликованной версии")
    private LocalDateTime toDate;

    public PostPublishRequest() {
        // Nothing to do.
    }

    public PostPublishRequest(String lastStorageCode, String oldStorageCode, String newStorageCode,
                              LocalDateTime fromDate, LocalDateTime toDate) {

        this.lastStorageCode = lastStorageCode;
        this.oldStorageCode = oldStorageCode;
        this.newStorageCode = newStorageCode;

        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public String getLastStorageCode() {
        return lastStorageCode;
    }

    public void setLastStorageCode(String lastStorageCode) {
        this.lastStorageCode = lastStorageCode;
    }

    public String getOldStorageCode() {
        return oldStorageCode;
    }

    public void setOldStorageCode(String oldStorageCode) {
        this.oldStorageCode = oldStorageCode;
    }

    public String getNewStorageCode() {
        return newStorageCode;
    }

    public void setNewStorageCode(String newStorageCode) {
        this.newStorageCode = newStorageCode;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }
}
