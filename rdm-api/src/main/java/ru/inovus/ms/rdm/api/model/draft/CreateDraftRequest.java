package ru.inovus.ms.rdm.api.model.draft;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.validation.AttributeValidation;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;

@ApiModel(value = "Модель создания черновика", description = "Набор данных для создания черновика")
public class CreateDraftRequest implements Serializable {

    private Integer refBookId;

    private Structure structure;

    private Map<String, Object> passport; // NOSONAR

    private Map<String, List<AttributeValidation>> validations; // NOSONAR

    private boolean referrerValidationRequired;

    public CreateDraftRequest() {
    }

    public CreateDraftRequest(Integer refBookId, Structure structure, Map<String, Object> passport,
                              Map<String, List<AttributeValidation>> validations) {
        this.refBookId = refBookId;
        this.structure = structure;
        this.passport = passport;

        this.validations = validations;
    }

    public CreateDraftRequest(Integer refBookId, Structure structure) {
        this(refBookId, structure, emptyMap(), emptyMap());
    }

    @ApiModelProperty(value = "Идентификатор справочника")
    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

    @ApiModelProperty(value = "Структура черновика")
    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    @ApiModelProperty(value = "Паспорт")
    public Map<String, Object> getPassport() {
        return passport;
    }

    public void setPassport(Map<String, Object> passport) {
        this.passport = passport;
    }

    @ApiModelProperty(value = "Пользовательские проверки")
    public Map<String, List<AttributeValidation>> getValidations() {
        return validations;
    }

    public void setValidations(Map<String, List<AttributeValidation>> validations) {
        this.validations = validations;
    }

    /**
     * Требование дополнительной валидации для структуры ссылочного справочника.
     * Дополнительная валидация необходима при создании черновика из файла.
     */
    public boolean getReferrerValidationRequired() {
        return referrerValidationRequired;
    }

    public void setReferrerValidationRequired(boolean referrerValidationRequired) {
        this.referrerValidationRequired = referrerValidationRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CreateDraftRequest that = (CreateDraftRequest) o;
        return Objects.equals(refBookId, that.refBookId) &&
                Objects.equals(structure, that.structure) &&
                Objects.equals(passport, that.passport) &&

                Objects.equals(validations, that.validations) &&
                Objects.equals(referrerValidationRequired, that.referrerValidationRequired);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refBookId, structure, passport, validations, referrerValidationRequired);
    }
}
