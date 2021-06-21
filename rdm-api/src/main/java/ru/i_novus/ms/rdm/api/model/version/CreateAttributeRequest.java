package ru.i_novus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.i_novus.ms.rdm.api.model.validation.AttributeValidation;

import java.util.List;

@ApiModel(value = "Модель создания атрибута черновика",
        description = "Набор входных параметров для создания атрибута черновика")
public class CreateAttributeRequest extends RefBookVersionAttribute implements DraftChangeRequest {

    @ApiModelProperty("Значение оптимистической блокировки версии")
    private Integer optLockValue;

    @ApiParam("Пользовательские проверки для атрибута")
    private List<AttributeValidation> validations;

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

    public CreateAttributeRequest(Integer optLockValue,
                                  Structure.Attribute attribute,
                                  Structure.Reference reference,
                                  List<AttributeValidation> validations) {
        this(optLockValue, attribute, reference);

        this.validations = validations;
    }

    @Override
    public Integer getOptLockValue() {
        return optLockValue;
    }

    @Override
    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public List<AttributeValidation> getValidations() {
        return validations;
    }

    public void setValidations(List<AttributeValidation> validations) {
        this.validations = validations;
    }
}