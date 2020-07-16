package ru.i_novus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.model.Structure;

import java.io.Serializable;
import java.util.Objects;

/** Модель атрибута версии справочника. */
public class RefBookVersionAttribute implements Serializable {

    @ApiModelProperty("Идентификатор версии")
    private Integer versionId;

    @ApiModelProperty("Атрибут версии")
    private Structure.Attribute attribute;

    @ApiModelProperty("Атрибут-ссылка версии")
    private Structure.Reference reference;

    @SuppressWarnings("WeakerAccess")
    public RefBookVersionAttribute() {}

    public RefBookVersionAttribute(Integer versionId,
                                   Structure.Attribute attribute,
                                   Structure.Reference reference) {
        this.versionId = versionId;
        this.attribute = attribute;
        this.reference = reference;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Structure.Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Structure.Attribute attribute) {
        this.attribute = attribute;
    }

    public Structure.Reference getReference() {
        return reference;
    }

    public void setReference(Structure.Reference reference) {
        this.reference = reference;
    }

    /* Полная проверка на атрибут-ссылку. */
    public boolean hasReference() {
        return attribute != null && attribute.isReferenceType() && reference != null;
    }

    /* Полная проверка на совпадение выражений для вычисления отображаемого ссылочного значения. */
    public boolean equalsReferenceDisplayExpression(RefBookVersionAttribute o) {
        return hasReference() && o != null && o.hasReference()
                && Objects.equals(reference.getDisplayExpression(), o.reference.getDisplayExpression());
    }
}