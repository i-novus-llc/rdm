package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("Справочник")
public class RefBook extends RefBookVersion {

    @ApiModelProperty("Признак возможности удаления")
    private Boolean removable;

    public RefBook() {
    }

    public RefBook(RefBookVersion refBookVersion) {
        super(refBookVersion);
    }

    public RefBook(RefBook refBook) {
        super(refBook);
        this.removable = refBook.getRemovable();
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }
}