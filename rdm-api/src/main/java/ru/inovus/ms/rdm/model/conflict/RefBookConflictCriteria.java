package ru.inovus.ms.rdm.model.conflict;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.List;

@ApiModel("Критерии поиска конфликта")
public class RefBookConflictCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор версии справочника со ссылками")
    @QueryParam("referrerVersionId")
    private Integer referrerVersionId;

    @ApiModelProperty("Идентификатор справочника со ссылками")
    @QueryParam("referrerVersionRefBookId")
    private Integer referrerVersionRefBookId;

    @ApiModelProperty("Идентификатор версии опубликованного справочника")
    @QueryParam("publishedVersionId")
    private Integer publishedVersionId;

    @ApiModelProperty("Идентификатор опубликованного справочника")
    @QueryParam("publishedVersionRefBookId")
    private Integer publishedVersionRefBookId;

    @ApiModelProperty("Признак последней опубликованной версии")
    @QueryParam("isLastPublishedVersion")
    private boolean isLastPublishedVersion;

    @ApiModelProperty("Системный идентификатор записи с конфликтом")
    @QueryParam("refRecordId")
    private Long refRecordId;

    @ApiModelProperty("Системные идентификаторы записи с конфликтом")
    @QueryParam("refRecordIds")
    List<Long> refRecordIds;

    @ApiModelProperty("Название поля-ссылки с конфликтом по отображаемому значению")
    @QueryParam("refFieldCode")
    private String refFieldCode;

    @ApiModelProperty("Тип конфликта")
    @QueryParam("conflictType")
    private ConflictType conflictType;

    @ApiModelProperty("Дата создания записи")
    @QueryParam("creationDate")
    private LocalDateTime creationDate;

    @SuppressWarnings("unused")
    public RefBookConflictCriteria() {
    }

    public RefBookConflictCriteria(Integer referrerVersionId, Integer publishedVersionId) {
        this.referrerVersionId = referrerVersionId;
        this.publishedVersionId = publishedVersionId;
    }

    public RefBookConflictCriteria(Integer referrerVersionId, Integer publishedVersionId, String refFieldCode, ConflictType conflictType) {
        this.referrerVersionId = referrerVersionId;
        this.publishedVersionId = publishedVersionId;
        this.refFieldCode = refFieldCode;
        this.conflictType = conflictType;
    }

    public Integer getReferrerVersionId() {
        return referrerVersionId;
    }

    public void setReferrerVersionId(Integer referrerVersionId) {
        this.referrerVersionId = referrerVersionId;
    }

    public Integer getReferrerVersionRefBookId() {
        return referrerVersionRefBookId;
    }

    public void setReferrerVersionRefBookId(Integer referrerVersionRefBookId) {
        this.referrerVersionRefBookId = referrerVersionRefBookId;
    }

    public Integer getPublishedVersionId() {
        return publishedVersionId;
    }

    public void setPublishedVersionId(Integer publishedVersionId) {
        this.publishedVersionId = publishedVersionId;
    }

    public Integer getPublishedVersionRefBookId() {
        return publishedVersionRefBookId;
    }

    public void setPublishedVersionRefBookId(Integer publishedVersionRefBookId) {
        this.publishedVersionRefBookId = publishedVersionRefBookId;
    }

    public boolean getIsLastPublishedVersion() {
        return isLastPublishedVersion;
    }

    public void setIsLastPublishedVersion(boolean isLastPublishedVersion) {
        this.isLastPublishedVersion = isLastPublishedVersion;
    }

    public Long getRefRecordId() {
        return refRecordId;
    }

    public void setRefRecordId(Long refRecordId) {
        this.refRecordId = refRecordId;
    }

    public List<Long> getRefRecordIds() {
        return refRecordIds;
    }

    public void setRefRecordIds(List<Long> refRecordIds) {
        this.refRecordIds = refRecordIds;
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
}
