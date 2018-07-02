package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.util.JsonDateSerializer;

import java.time.LocalDateTime;

@ApiModel("Версия справочника")
public class RefBookVersion {

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

    @ApiModelProperty("Аннотация")
    private String annotation;

    @ApiModelProperty("Комментарий к версии")
    private String comment;

    @ApiModelProperty("Версия")
    private String version;

    @ApiModelProperty("Версия (отображаемое значение)")
    private String displayVersion;

    @ApiModelProperty("Дата публикации")
    @JsonSerialize(using = JsonDateSerializer.class)
    private LocalDateTime fromDate;

    @ApiModelProperty("Дата окончания действия")
    @JsonSerialize(using = JsonDateSerializer.class)
    private LocalDateTime toDate;

    @ApiModelProperty("Статус версии")
    private RefBookVersionStatus status;

    @ApiModelProperty("Статус версии (отображаемое значение)")
    private String displayStatus;

    @ApiModelProperty("В архиве")
    private Boolean archived;

    @ApiModelProperty("Справочник содержит версию со статусом 'Публикуется'")
    private Boolean refBookHasPublishingVersion;


    public RefBookVersion() {
    }

    public RefBookVersion(RefBookVersion refBookVersion) {
        this.id = refBookVersion.getId();
        this.refBookId = refBookVersion.getRefBookId();
        this.code = refBookVersion.getCode();
        this.shortName = refBookVersion.getShortName();
        this.fullName = refBookVersion.getFullName();
        this.annotation = refBookVersion.getAnnotation();
        this.comment = refBookVersion.getComment();
        this.version = refBookVersion.getVersion();
        this.displayVersion = refBookVersion.getDisplayVersion();
        this.fromDate = refBookVersion.getFromDate();
        this.toDate = refBookVersion.getToDate();
        this.status = refBookVersion.getStatus();
        this.displayStatus = refBookVersion.getDisplayStatus();
        this.archived = refBookVersion.getArchived();
        this.refBookHasPublishingVersion = refBookVersion.getRefBookHasPublishingVersion();
    }

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDisplayVersion() {
        return displayVersion;
    }

    public void setDisplayVersion(String displayVersion) {
        this.displayVersion = displayVersion;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }

    public RefBookVersionStatus getStatus() {
        return status;
    }

    public void setStatus(RefBookVersionStatus status) {
        this.status = status;
    }

    public String getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(String displayStatus) {
        this.displayStatus = displayStatus;
    }

    public Boolean getArchived() {
        return archived != null && archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Boolean getRefBookHasPublishingVersion() {
        return refBookHasPublishingVersion;
    }

    public void setRefBookHasPublishingVersion(Boolean refBookHasPublishingVersion) {
        this.refBookHasPublishingVersion = refBookHasPublishingVersion;
    }
}