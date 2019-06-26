package ru.inovus.ms.rdm.model.draft;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.model.Structure;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@ApiModel(value = "Модель создания черновика", description = "Набор данных для создания черновика")
public class CreateDraftRequest implements Serializable {

    private Integer refBookId;

    private Structure structure;

    private Map<String, String> passport;

    public CreateDraftRequest(Integer refBookId, Structure structure, Map<String, String> passport) {
        this.refBookId = refBookId;
        this.structure = structure;
        this.passport = passport;
    }

    public CreateDraftRequest(Integer refBookId, Structure structure) {
        this.refBookId = refBookId;
        this.structure = structure;
    }

    public CreateDraftRequest() {
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
    public Map<String, String> getPassport() {
        return passport;
    }

    public void setPassport(Map<String, String> passport) {
        this.passport = passport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateDraftRequest that = (CreateDraftRequest) o;
        return Objects.equals(refBookId, that.refBookId) &&
                Objects.equals(structure, that.structure) &&
                Objects.equals(passport, that.passport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refBookId, structure, passport);
    }
}
