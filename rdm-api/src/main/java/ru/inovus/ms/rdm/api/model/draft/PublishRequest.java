package ru.inovus.ms.rdm.api.model.draft;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.time.LocalDateTime;

@ApiModel(value = "Модель публикации черновика",
        description = "Набор входных параметров для публикации черновика")
public class PublishRequest implements Serializable {

    @ApiModelProperty(value = "Идентификатор черновика")
    private Integer draftId;

    @ApiParam("Версия, под которой публикуется черновик")
    private String versionName;
    @ApiParam("Дата начала действия опубликованной версии")
    private LocalDateTime fromDate;
    @ApiParam("Дата окончания действия опубликованной версии")
    private LocalDateTime toDate;

    @ApiParam("Признак разрешения конфликтов")
    private boolean resolveConflicts;

    public PublishRequest() {
    }

    public PublishRequest(Integer draftId) {
        this.draftId = draftId;
    }

    public Integer getDraftId() {
        return draftId;
    }

    public void setDraftId(Integer draftId) {
        this.draftId = draftId;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
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

    public boolean getResolveConflicts() {
        return resolveConflicts;
    }

    public void setResolveConflicts(boolean resolveConflicts) {
        this.resolveConflicts = resolveConflicts;
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
