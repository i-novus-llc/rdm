package ru.inovus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель удаления атрибута черновика",
        description = "Набор входных параметров для удаления атрибута черновика")
public class DeleteAttributeRequest implements DraftChangeRequest {

    @ApiModelProperty("Идентификатор версии-черновика")
    private Integer versionId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    @ApiModelProperty("Код атрибута версии-черновика")
    private String attributeCode;

    public DeleteAttributeRequest() {
    }

    public DeleteAttributeRequest(Integer versionId, Integer optLockValue, String attributeCode) {
        this.versionId = versionId;
        this.optLockValue = optLockValue;

        this.attributeCode = attributeCode;
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

    public String getAttributeCode() {
        return attributeCode;
    }

    public void setAttributeCode(String code) {
        this.attributeCode = code;
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
