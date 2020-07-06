package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель удаления всех записей черновика",
        description = "Набор входных параметров для всех записей черновика")
public class DeleteAllDataRequest implements DraftChangeRequest {

    @ApiModelProperty("Идентификатор версии-черновика")
    private Integer versionId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    public DeleteAllDataRequest() {
    }

    public DeleteAllDataRequest(Integer versionId, Integer optLockValue) {
        this.versionId = versionId;
        this.optLockValue = optLockValue;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer draftId) {
        this.versionId = draftId;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
