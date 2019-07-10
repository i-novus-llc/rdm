package ru.inovus.ms.rdm.model.conflict;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

@ApiModel("Критерии удаления конфликта")
public class DeleteRefBookConflictCriteria extends AbstractCriteria {

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
    @QueryParam("publishedVersionId")
    private Integer publishedVersionRefBookId;

    @ApiModelProperty("Идентификатор исключаемой версии опубликованного справочника")
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

    public Integer getExcludedPublishedVersionId() {
        return excludedPublishedVersionId;
    }

    public void setExcludedPublishedVersionId(Integer excludedPublishedVersionId) {
        this.excludedPublishedVersionId = excludedPublishedVersionId;
    }
}
