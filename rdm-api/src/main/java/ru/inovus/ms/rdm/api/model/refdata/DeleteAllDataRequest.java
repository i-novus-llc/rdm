package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель удаления всех записей черновика",
        description = "Набор входных параметров для удаления всех записей черновика")
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

    @Override
    public Integer getVersionId() {
        return versionId;
    }

    @Override
    public void setVersionId(Integer draftId) {
        this.versionId = draftId;
    }

    @Override
    public Integer getOptLockValue() {
        return optLockValue;
    }

    @Override
    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
