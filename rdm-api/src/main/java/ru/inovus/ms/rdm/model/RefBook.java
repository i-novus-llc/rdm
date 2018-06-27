package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.util.JsonDateSerializer;

import java.time.LocalDateTime;

@ApiModel("Справочник")
public class RefBook {

    @ApiModelProperty("Идентификатор версии")
    private Integer id;

    @ApiModelProperty("Идентификатор справочника")
    private Integer refBookId;

    @ApiModelProperty("Код")
    private String code;

    @ApiModelProperty("Краткое наименование")
    private String shortName;

    @ApiModelProperty("Полное наименование")
    private String fullName;

    @ApiModelProperty("Версия")
    private String version;

    @ApiModelProperty("Дата последней публикации")
    @JsonSerialize(using = JsonDateSerializer.class)
    private LocalDateTime fromDate;

    @ApiModelProperty("Аннотация")
    private String annotation;

    @ApiModelProperty("Комментарий к версии")
    private String comment;

    @ApiModelProperty("Признак возможности удаления")
    private Boolean removable;

    @ApiModelProperty("Статус версии")
    private RefBookVersionStatus status;

    @ApiModelProperty("Архив")
    private Boolean archived;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }

    public RefBookVersionStatus getStatus() {
        return status;
    }

    public void setStatus(RefBookVersionStatus status) {
        this.status = status;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }
}