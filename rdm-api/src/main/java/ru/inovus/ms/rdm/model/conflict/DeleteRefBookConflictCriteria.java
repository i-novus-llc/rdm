package ru.inovus.ms.rdm.model.conflict;

import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

/**
 * Критерий удаления конфликтов.
 */
public class DeleteRefBookConflictCriteria extends AbstractCriteria {

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
    @QueryParam("publishedVersionId")
    private Integer publishedVersionRefBookId;

    @ApiParam("Название поля-ссылки с конфликтом по отображаемому значению")
    @QueryParam("refFieldCode")
    private String refFieldCode;

    @ApiParam("Тип конфликта")
    @QueryParam("conflictType")
    private ConflictType conflictType;

    @ApiParam("Идентификатор исключаемой версии опубликованного справочника")
    @QueryParam("excludedPublishedVersionId")
    private Integer excludedPublishedVersionId;

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

    public Integer getExcludedPublishedVersionId() {
        return excludedPublishedVersionId;
    }

    public void setExcludedPublishedVersionId(Integer excludedPublishedVersionId) {
        this.excludedPublishedVersionId = excludedPublishedVersionId;
    }
}
