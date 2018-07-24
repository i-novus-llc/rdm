package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.util.JsonDateSerializer;

import java.time.LocalDateTime;

@ApiModel("Справочник")
public class RefBook extends RefBookVersion {

    @ApiModelProperty("Признак возможности удаления")
    private Boolean removable;

    @ApiModelProperty("Дата публикации последней версии")
    @JsonSerialize(using = JsonDateSerializer.class)
    private LocalDateTime lastPublishedVersionFromDate;

    public RefBook() {
    }

    public RefBook(RefBookVersion refBookVersion) {
        super(refBookVersion);
    }

    public RefBook(RefBook refBook) {
        super(refBook);
        this.removable = refBook.getRemovable();
        this.lastPublishedVersionFromDate = refBook.getLastPublishedVersionFromDate();
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

    public String getCodeName() {
        return getCode() + " " + getShortName();
    }
}