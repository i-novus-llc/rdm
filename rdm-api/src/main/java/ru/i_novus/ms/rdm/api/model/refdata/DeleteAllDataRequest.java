package ru.i_novus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель удаления всех записей черновика",
        description = "Набор входных параметров для удаления всех записей черновика")
public class DeleteAllDataRequest implements DraftChangeRequest {

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    public DeleteAllDataRequest() {
        // Nothing to do.
    }

    public DeleteAllDataRequest(Integer optLockValue) {
        this.optLockValue = optLockValue;
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
        return JsonUtil.toJsonString(this);
    }
}
