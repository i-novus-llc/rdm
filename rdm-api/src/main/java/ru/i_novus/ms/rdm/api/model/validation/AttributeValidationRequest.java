package ru.i_novus.ms.rdm.api.model.validation;

import ru.i_novus.ms.rdm.api.model.version.RefBookVersionAttribute;

import java.util.List;

/**
 * Запрос на обновление пользовательских проверок атрибута.
 */
public class AttributeValidationRequest {

    /** Старый атрибут. */
    private RefBookVersionAttribute oldAttribute;

    /** Новый атрибут. */
    private RefBookVersionAttribute newAttribute;

    /** Пользовательские проверки для нового атрибута. */
    private List<AttributeValidation> validations;

    public AttributeValidationRequest() {
    }

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
