package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.time.LocalDateTime;

@ApiModel("Конфликт")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefBookConflict {

    @ApiModelProperty("Идентификатор записи о конфликте")
    private Integer id;

    @ApiModelProperty("Идентификатор версии справочника со ссылками")
    private Integer referrerVersionId;

    @ApiModelProperty("Идентификатор версии опубликованного справочника")
    private Integer publishedVersionId;

    @ApiModelProperty("Системный идентификатор записи с конфликтом")
    private Integer refRecordId;

    @ApiModelProperty("Название поля-ссылки с конфликтом по отображаемому значению")
    private String refFieldCode;

    @ApiModelProperty("Тип конфликта")
    private ConflictType conflictType;

    @ApiModelProperty("Дата создания записи")
    private LocalDateTime creationDate;

    @ApiModelProperty("Дата обработки записи")
    private LocalDateTime handlingDate;

    public RefBookConflict() {
    }

    public RefBookConflict(Integer id, Integer referrerVersionId, Integer publishedVersionId, Integer refRecordId,
                           String refFieldCode, ConflictType conflictType,
                           LocalDateTime creationDate, LocalDateTime handlingDate) {
        this.id = id;
        this.referrerVersionId = referrerVersionId;
        this.publishedVersionId = publishedVersionId;
        this.refRecordId = refRecordId;
        this.refFieldCode = refFieldCode;
        this.conflictType = conflictType;
        this.creationDate = creationDate;
        this.handlingDate = handlingDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getReferrerVersionId() {
        return referrerVersionId;
    }

    public void setReferrerVersionId(Integer referrerVersionId) {
        this.referrerVersionId = referrerVersionId;
    }

    public Integer getPublishedVersionId() {
        return publishedVersionId;
    }

    public void setPublishedVersionId(Integer publishedVersionId) {
        this.publishedVersionId = publishedVersionId;
    }

    public Integer getRefRecordId() {
        return refRecordId;
    }

    public void setRefRecordId(Integer refRecordId) {
        this.refRecordId = refRecordId;
    }

    public String getRefFieldCode() {
        return refFieldCode;
    }

    public void setRefFieldCode(String refFieldCode) {
        this.refFieldCode = refFieldCode;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public void setConflictType(ConflictType conflictType) {
        this.conflictType = conflictType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getHandlingDate() {
        return handlingDate;
    }

    public void setHandlingDate(LocalDateTime handlingDate) {
        this.handlingDate = handlingDate;
    }
}
