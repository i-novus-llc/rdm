package ru.i_novus.ms.rdm.api.model.draft;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.time.LocalDateTime;

@ApiModel(value = "Модель публикации черновика",
        description = "Набор входных параметров для публикации черновика")
public class PublishRequest implements DraftChangeRequest {

    @ApiModelProperty("Значение оптимистической блокировки версии")
    private Integer optLockValue;

    @ApiModelProperty("Версия, под которой публикуется черновик")
    private String versionName;

    @ApiModelProperty("Дата начала действия опубликованной версии")
    private LocalDateTime fromDate;

    @ApiModelProperty("Дата окончания действия опубликованной версии")
    private LocalDateTime toDate;

    @ApiModelProperty("Признак разрешения конфликтов")
    private boolean resolveConflicts;

    public PublishRequest() {
        // Nothing to do.
    }

    public PublishRequest(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
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
        return JsonUtil.toJsonString(this);
    }
}
