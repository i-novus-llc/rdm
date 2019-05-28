package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.RefBookOperation;

import java.time.LocalDateTime;

@ApiModel("Справочник")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefBook extends RefBookVersion {

    @ApiModelProperty("Признак возможности удаления")
    private Boolean removable;

    @ApiModelProperty("Категория")
    private String category;

    @ApiModelProperty("Последняя опубликованная версия")
    private String lastPublishedVersion;

    @ApiModelProperty("Дата публикации последней версии")
    private LocalDateTime lastPublishedVersionFromDate;

    @ApiModelProperty("Текущая операция над справочником")
    private RefBookOperation currentOperation;

    @ApiModelProperty("Наличие первичного ключа")
    private Boolean hasPrimaryAttribute;

    public RefBook() {
    }

    public RefBook(RefBookVersion refBookVersion) {
        super(refBookVersion);
    }

    public RefBook(RefBook refBook) {
        super(refBook);

        this.removable = refBook.getRemovable();
        this.lastPublishedVersion = refBook.getLastPublishedVersion();
        this.lastPublishedVersionFromDate = refBook.getLastPublishedVersionFromDate();
        this.currentOperation = refBook.getCurrentOperation();
        this.hasPrimaryAttribute = refBook.getHasPrimaryAttribute();
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
}