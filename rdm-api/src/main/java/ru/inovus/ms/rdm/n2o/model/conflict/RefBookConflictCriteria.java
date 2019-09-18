package ru.inovus.ms.rdm.n2o.model.conflict;

import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.n2o.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Критерий поиска конфликтов.
 */
public class RefBookConflictCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор версии справочника со ссылками")
    @QueryParam("referrerVersionId")
    private Integer referrerVersionId;

    @ApiParam("Идентификатор справочника со ссылками")
    @QueryParam("referrerVersionRefBookId")
    private Integer referrerVersionRefBookId;

    @ApiParam("Идентификатор версии опубликованного справочника")
    @QueryParam("publishedVersionId")
    private Integer publishedVersionId;

    @ApiParam("Идентификатор опубликованного справочника")
    @QueryParam("publishedVersionRefBookId")
    private Integer publishedVersionRefBookId;

    @ApiParam("Признак последней опубликованной версии")
    @QueryParam("isLastPublishedVersion")
    private boolean isLastPublishedVersion;

    @ApiParam("Системный идентификатор записи с конфликтом")
    @QueryParam("refRecordId")
    private Long refRecordId;

    @ApiParam("Системные идентификаторы записи с конфликтом")
    @QueryParam("refRecordIds")
    List<Long> refRecordIds;

    @ApiParam("Название поля-ссылки с конфликтом по отображаемому значению")
    @QueryParam("refFieldCode")
    private String refFieldCode;

    @ApiParam("Тип конфликта")
    @QueryParam("conflictType")
    private ConflictType conflictType;

    @ApiParam("Типы конфликтов")
    @QueryParam("conflictTypes")
    private List<ConflictType> conflictTypes;

    @ApiParam("Дата создания записи")
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

    public List<ConflictType> getConflictTypes() {
        return conflictTypes;
    }

    public void setConflictTypes(List<ConflictType> conflictTypes) {
        this.conflictTypes = conflictTypes;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
