package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.RefBookOperation;
import ru.inovus.ms.rdm.util.JsonLocalDateTimeSerializer;
import ru.inovus.ms.rdm.util.JsonLocalDateTimeDeserializer;

import java.time.LocalDateTime;

@ApiModel("Справочник")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefBook extends RefBookVersion {

    @ApiModelProperty("Признак возможности удаления")
    private Boolean removable;

    @ApiModelProperty("Категория")
    private String category;

    @ApiModelProperty("Дата публикации последней версии")
    @JsonSerialize(using = JsonLocalDateTimeSerializer.class)
    @JsonDeserialize(using = JsonLocalDateTimeDeserializer.class)
    private LocalDateTime lastPublishedVersionFromDate;

    @ApiModelProperty("Текущая операция над справочником")
    private RefBookOperation currentOperation;

    public RefBook() {
    }

    public RefBook(RefBookVersion refBookVersion) {
        super(refBookVersion);
    }

    public RefBook(RefBook refBook) {
        super(refBook);
        this.removable = refBook.getRemovable();
        this.lastPublishedVersionFromDate = refBook.getLastPublishedVersionFromDate();
        this.currentOperation = refBook.getCurrentOperation();
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}