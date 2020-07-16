package ru.i_novus.ms.rdm.api.model.validation;

import ru.i_novus.ms.rdm.api.model.version.RefBookVersionAttribute;

import java.util.List;

public class AttributeValidationRequest {

    private RefBookVersionAttribute oldAttribute;
    private RefBookVersionAttribute newAttribute;

    private List<AttributeValidation> validations;

    public AttributeValidationRequest() {
    }

    @SuppressWarnings("unused")
    public AttributeValidationRequest(RefBookVersionAttribute oldAttribute,
                                      RefBookVersionAttribute newAttribute,
                                      List<AttributeValidation> validations) {
        this.oldAttribute = oldAttribute;
        this.newAttribute = newAttribute;
        this.validations = validations;
    }

    public RefBookVersionAttribute getOldAttribute() {
        return oldAttribute;
    }

    public void setOldAttribute(RefBookVersionAttribute oldAttribute) {
        this.oldAttribute = oldAttribute;
    }

    public RefBookVersionAttribute getNewAttribute() {
        return newAttribute;
    }

    public void setNewAttribute(RefBookVersionAttribute newAttribute) {
        this.newAttribute = newAttribute;
    }

    public List<AttributeValidation> getValidations() {
        return validations;
    }

    public void setValidations(List<AttributeValidation> validations) {
        this.validations = validations;
    }
}
