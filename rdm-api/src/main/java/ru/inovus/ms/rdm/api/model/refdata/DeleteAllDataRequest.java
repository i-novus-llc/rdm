package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель удаления всех записей черновика",
        description = "Набор входных параметров для всех записей черновика")
public class DeleteAllDataRequest implements DraftChangeRequest {

    @ApiModelProperty("Идентификатор черновика")
    private Integer draftId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    public DeleteAllDataRequest() {
    }

    public DeleteAllDataRequest(Integer draftId, Integer optLockValue) {
        this.draftId = draftId;
        this.optLockValue = optLockValue;
    }

    public Integer getDraftId() {
        return draftId;
    }

    public void setDraftId(Integer draftId) {
        this.draftId = draftId;
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
