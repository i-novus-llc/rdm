package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.util.JsonDateSerializer;

import java.time.LocalDateTime;

public class Passport extends RefBook {

    @ApiModelProperty("Количество записей в версии")
    private Integer recordsCount;

    @ApiModelProperty("Дата публикации первой версии")
    @JsonSerialize(using = JsonDateSerializer.class)
    private LocalDateTime firstPublishedVersionFromDate;

    public Passport() {
    }

    public Passport(RefBook refBook) {
        super(refBook);
    }

    public Integer getRecordsCount() {
        return recordsCount;
    }

    public void setRecordsCount(Integer recordsCount) {
        this.recordsCount = recordsCount;
    }

    public LocalDateTime getFirstPublishedVersionFromDate() {
        return firstPublishedVersionFromDate;
    }

    public void setFirstPublishedVersionFromDate(LocalDateTime firstPublishedVersionFromDate) {
        this.firstPublishedVersionFromDate = firstPublishedVersionFromDate;
    }
}
