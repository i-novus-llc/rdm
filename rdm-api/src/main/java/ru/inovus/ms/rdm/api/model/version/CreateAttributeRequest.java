package ru.inovus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refdata.DraftChangeRequest;

@ApiModel(value = "Модель создания атрибута черновика",
        description = "Набор входных параметров для создания атрибута черновика")
public class CreateAttributeRequest extends RefBookVersionAttribute implements DraftChangeRequest {

    @ApiModelProperty("Значение оптимистической блокировки версии")
    private Integer optLockValue;

    @SuppressWarnings("unused")
    public CreateAttributeRequest() {
        // Nothing to do.
    }

    public CreateAttributeRequest(Integer optLockValue,
                                  Structure.Attribute attribute,
                                  Structure.Reference reference) {
        super(null, attribute, reference);
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
}