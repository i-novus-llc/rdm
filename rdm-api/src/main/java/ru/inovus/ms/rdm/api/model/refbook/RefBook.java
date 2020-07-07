package ru.inovus.ms.rdm.api.model.refbook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.enumeration.RefBookOperation;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;

import java.time.LocalDateTime;

@ApiModel("Справочник")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefBook extends RefBookVersion {

    @ApiModelProperty("Признак возможности удаления")
    private Boolean removable;

    @ApiModelProperty("Категория")
    private String category;

    @ApiModelProperty("Идентификатор черновика")
    private Integer draftVersionId;

    @ApiModelProperty("Идентификатор последней опубликованной версии")
    private Integer lastPublishedVersionId;

    @ApiModelProperty("Последняя опубликованная версия")
    private String lastPublishedVersion;

    @ApiModelProperty("Дата публикации последней версии")
    private LocalDateTime lastPublishedVersionFromDate;

    @ApiModelProperty("Текущая операция над справочником")
    private RefBookOperation currentOperation;

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

    @ApiModelProperty("Публикуется ли данный справочник в данный момент")
    private Boolean publishing;

    @ApiModelProperty("Обновляется ли данный справочник в данный момент")
    private Boolean updating;

    public RefBook() {
    }

    public RefBook(RefBookVersion refBookVersion) {
        super(refBookVersion);
    }

    public RefBook(RefBook refBook) {
        super(refBook);

        this.removable = refBook.getRemovable();

        this.draftVersionId = refBook.getDraftVersionId();
        this.lastPublishedVersionId = refBook.getLastPublishedVersionId();
        this.lastPublishedVersion = refBook.getLastPublishedVersion();
        this.lastPublishedVersionFromDate = refBook.getLastPublishedVersionFromDate();

        this.currentOperation = refBook.getCurrentOperation();
        this.hasPrimaryAttribute = refBook.getHasPrimaryAttribute();

        this.hasDataConflict = refBook.getHasDataConflict();
        this.hasUpdatedConflict = refBook.getHasUpdatedConflict();
        this.hasAlteredConflict = refBook.getHasAlteredConflict();
        this.hasStructureConflict = refBook.getHasStructureConflict();

        this.lastHasConflict = refBook.getLastHasConflict();
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public RefBookOperation getCurrentOperation() {
        return currentOperation;
    }

    public void setCurrentOperation(RefBookOperation currentOperation) {
        this.currentOperation = currentOperation;
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

    public Boolean getPublishing() {
        return publishing;
    }

    public void setPublishing(Boolean publishing) {
        this.publishing = publishing;
    }

    public Boolean getUpdating() {
        return updating;
    }

    public void setUpdating(Boolean updating) {
        this.updating = updating;
    }
}