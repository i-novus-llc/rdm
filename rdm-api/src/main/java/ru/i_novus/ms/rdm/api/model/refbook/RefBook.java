package ru.i_novus.ms.rdm.api.model.refbook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.enumeration.RefBookOperation;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.time.LocalDateTime;
import java.util.Objects;

@ApiModel("Справочник")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefBook extends RefBookVersion {

    @ApiModelProperty("Текущая операция над справочником")
    private RefBookOperation currentOperation;

    @ApiModelProperty("Признак возможности удаления")
    private Boolean removable;

    @ApiModelProperty("Идентификатор черновика")
    private Integer draftVersionId;

    @ApiModelProperty("Идентификатор последней опубликованной версии")
    private Integer lastPublishedVersionId;

    @ApiModelProperty("Последняя опубликованная версия")
    private String lastPublishedVersion;

    @ApiModelProperty("Дата публикации последней версии")
    private LocalDateTime lastPublishedVersionFromDate;

    @ApiModelProperty("Наличие первичного ключа")
    private Boolean hasPrimaryAttribute;

    @ApiModelProperty("Наличие связанного справочника")
    private Boolean hasReferrer;

    @ApiModelProperty("Наличие конфликта данных")
    private Boolean hasDataConflict;

    @ApiModelProperty("Наличие конфликта обновления записи")
    private Boolean hasUpdatedConflict;

    @ApiModelProperty("Наличие конфликта изменения структуры")
    private Boolean hasAlteredConflict;

    @ApiModelProperty("Наличие конфликта структуры")
    private Boolean hasStructureConflict;

    @ApiModelProperty("Наличие конфликта в последней опубликованной версии")
    private Boolean lastHasConflict;

    public RefBook() {
    }

    public RefBook(RefBookVersion refBookVersion) {
        super(refBookVersion);
    }

    public RefBook(RefBook refBook) {
        super(refBook);

        this.currentOperation = refBook.currentOperation;
        this.removable = refBook.removable;

        this.draftVersionId = refBook.draftVersionId;
        this.lastPublishedVersionId = refBook.lastPublishedVersionId;
        this.lastPublishedVersion = refBook.lastPublishedVersion;
        this.lastPublishedVersionFromDate = refBook.lastPublishedVersionFromDate;

        this.hasPrimaryAttribute = refBook.hasPrimaryAttribute;
        this.hasReferrer = refBook.hasReferrer;

        this.hasDataConflict = refBook.hasDataConflict;

        this.hasUpdatedConflict = refBook.hasUpdatedConflict;
        this.hasAlteredConflict = refBook.hasAlteredConflict;
        this.hasStructureConflict = refBook.hasStructureConflict;

        this.lastHasConflict = refBook.lastHasConflict;
    }

    public RefBookOperation getCurrentOperation() {
        return currentOperation;
    }

    public void setCurrentOperation(RefBookOperation currentOperation) {
        this.currentOperation = currentOperation;
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }

    public Integer getDraftVersionId() {
        return draftVersionId;
    }

    public void setDraftVersionId(Integer draftVersionId) {
        this.draftVersionId = draftVersionId;
    }

    public Integer getLastPublishedVersionId() {
        return lastPublishedVersionId;
    }

    public void setLastPublishedVersionId(Integer lastPublishedVersionId) {
        this.lastPublishedVersionId = lastPublishedVersionId;
    }

    public String getLastPublishedVersion() {
        return lastPublishedVersion;
    }

    public void setLastPublishedVersion(String lastPublishedVersion) {
        this.lastPublishedVersion = lastPublishedVersion;
    }

    public LocalDateTime getLastPublishedVersionFromDate() {
        return lastPublishedVersionFromDate;
    }

    public void setLastPublishedVersionFromDate(LocalDateTime lastPublishedVersionFromDate) {
        this.lastPublishedVersionFromDate = lastPublishedVersionFromDate;
    }

    public Boolean getHasPrimaryAttribute() {
        return hasPrimaryAttribute;
    }

    public void setHasPrimaryAttribute(Boolean hasPrimaryAttribute) {
        this.hasPrimaryAttribute = hasPrimaryAttribute;
    }

    public Boolean getHasReferrer() {
        return hasReferrer;
    }

    public void setHasReferrer(Boolean hasReferrer) {
        this.hasReferrer = hasReferrer;
    }

    public Boolean getHasDataConflict() {
        return hasDataConflict;
    }

    public void setHasDataConflict(Boolean hasDataConflict) {
        this.hasDataConflict = hasDataConflict;
    }

    public Boolean getHasUpdatedConflict() {
        return hasUpdatedConflict;
    }

    public void setHasUpdatedConflict(Boolean hasUpdatedConflict) {
        this.hasUpdatedConflict = hasUpdatedConflict;
    }

    public Boolean getHasAlteredConflict() {
        return hasAlteredConflict;
    }

    public void setHasAlteredConflict(Boolean hasAlteredConflict) {
        this.hasAlteredConflict = hasAlteredConflict;
    }

    public Boolean getHasStructureConflict() {
        return hasStructureConflict;
    }

    public void setHasStructureConflict(Boolean hasStructureConflict) {
        this.hasStructureConflict = hasStructureConflict;
    }

    public Boolean getLastHasConflict() {
        return lastHasConflict;
    }

    public void setLastHasConflict(Boolean lastHasConflict) {
        this.lastHasConflict = lastHasConflict;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RefBook that = (RefBook) o;
        return Objects.equals(currentOperation, that.currentOperation) &&
                Objects.equals(removable, that.removable) &&
                Objects.equals(draftVersionId, that.draftVersionId) &&
                Objects.equals(lastPublishedVersionId, that.lastPublishedVersionId) &&
                Objects.equals(lastPublishedVersion, that.lastPublishedVersion) &&
                Objects.equals(lastPublishedVersionFromDate, that.lastPublishedVersionFromDate) &&
                Objects.equals(hasPrimaryAttribute, that.hasPrimaryAttribute) &&
                Objects.equals(hasReferrer, that.hasReferrer) &&
                Objects.equals(hasDataConflict, that.hasDataConflict) &&
                Objects.equals(hasUpdatedConflict, that.hasUpdatedConflict) &&
                Objects.equals(hasAlteredConflict, that.hasAlteredConflict) &&
                Objects.equals(hasStructureConflict, that.hasStructureConflict) &&
                Objects.equals(lastHasConflict, that.lastHasConflict);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                currentOperation, removable,
                draftVersionId, lastPublishedVersionId,
                lastPublishedVersion, lastPublishedVersionFromDate,
                hasPrimaryAttribute, hasReferrer,
                hasDataConflict,
                hasUpdatedConflict, hasAlteredConflict, hasStructureConflict,
                lastHasConflict);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}