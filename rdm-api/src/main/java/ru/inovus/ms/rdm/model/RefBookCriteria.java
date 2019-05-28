package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.RefBookInfo;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@ApiModel("Критерии поиска справочника")
public class RefBookCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор справочника")
    @QueryParam("refBookId")
    private List<Integer> refBookIds;

    @ApiModelProperty("Код")
    @QueryParam("code")
    private String code;

    @ApiModelProperty("Дата последней публикации")
    @QueryParam("fromDateBegin")
    private LocalDateTime fromDateBegin;

    @ApiModelProperty("Дата последней публикации")
    @QueryParam("fromDateEnd")
    private LocalDateTime fromDateEnd;

    @ApiModelProperty("Источник данных справочника")
    @QueryParam("refBookInfo")
    private RefBookInfo refBookInfo;

    @ApiModelProperty("Категория")
    @QueryParam("category")
    private String category;

    @ApiModelProperty("В архиве")
    @QueryParam("isArchived")
    private boolean isArchived;

    @ApiModelProperty("Справочник опубликован")
    @QueryParam("hasPublished")
    private boolean hasPublished;

    @ApiModelProperty("У справочника есть черновик")
    @QueryParam("hasDraft")
    private boolean hasDraft;

    @ApiModelProperty("Наличие опубликованной версии")
    @QueryParam("hasPublishedVersion")
    private boolean hasPublishedVersion;

    @ApiModelProperty("Наличие первичного ключа")
    @QueryParam("hasPrimaryAttribute")
    private boolean hasPrimaryAttribute;

    @ApiModelProperty("Паспорт справочника")
    @QueryParam("passport")
    private Map<String, String> passport;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getFromDateBegin() {
        return fromDateBegin;
    }

    public void setFromDateBegin(LocalDateTime fromDateBegin) {
        this.fromDateBegin = fromDateBegin;
    }

    public LocalDateTime getFromDateEnd() {
        return fromDateEnd;
    }

    public void setFromDateEnd(LocalDateTime fromDateEnd) {
        this.fromDateEnd = fromDateEnd;
    }

    public RefBookInfo getRefBookInfo() {
        return refBookInfo;
    }

    public void setRefBookInfo(RefBookInfo refBookInfo) {
        this.refBookInfo = refBookInfo;
    }

    public Map<String, String> getPassport() {
        return passport;
    }

    public void setPassport(Map<String, String> passport) {
        this.passport = passport;
    }

    public boolean getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }

    public boolean getHasPublished() {
        return hasPublished;
    }

    public void setHasPublished(boolean hasPublished) {
        this.hasPublished = hasPublished;
    }

    public boolean getHasDraft() {
        return hasDraft;
    }

    public void setHasDraft(boolean hasDraft) {
        this.hasDraft = hasDraft;
    }

    public boolean getHasPublishedVersion() {
        return hasPublishedVersion;
    }

    public void setHasPublishedVersion(boolean hasPublishedVersion) {
        this.hasPublishedVersion = hasPublishedVersion;
    }

    public boolean getHasPrimaryAttribute() {
        return hasPrimaryAttribute;
    }

    public void setHasPrimaryAttribute(boolean hasPrimaryAttribute) {
        this.hasPrimaryAttribute = hasPrimaryAttribute;
    }

    public List<Integer> getRefBookIds() {
        return refBookIds;
    }

    public void setRefBookIds(List<Integer> refBookIds) {
        this.refBookIds = refBookIds;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
