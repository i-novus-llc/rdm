package ru.inovus.ms.rdm.model.refbook;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@ApiModel("Критерии поиска справочника")
@SuppressWarnings("unused")
public class RefBookCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор справочника")
    @QueryParam("refBookId")
    private List<Integer> refBookIds;

    @ApiModelProperty("Код")
    @QueryParam("code")
    private String code;

    @ApiModelProperty("Версия для исключения справочника")
    @QueryParam("excludeByVersionId")
    private Integer excludeByVersionId;

    @ApiModelProperty("Дата последней публикации")
    @QueryParam("fromDateBegin")
    private LocalDateTime fromDateBegin;

    @ApiModelProperty("Дата последней публикации")
    @QueryParam("fromDateEnd")
    private LocalDateTime fromDateEnd;

    @ApiModelProperty("Тип источника данных")
    @QueryParam("sourceType")
    private RefBookSourceType sourceType;

    @ApiModelProperty("Категория")
    @QueryParam("category")
    private String category;

    @ApiModelProperty("В архиве")
    @QueryParam("isArchived")
    private boolean isArchived;

    @ApiModelProperty("Не в архиве")
    @QueryParam("isNotArchived")
    private boolean isNotArchived;

    @ApiModelProperty("Справочник опубликован")
    @QueryParam("hasPublished")
    private boolean hasPublished;

    @ApiModelProperty("Наличие черновика")
    @QueryParam("hasDraft")
    private boolean hasDraft;

    @ApiModelProperty("Наличие опубликованной версии")
    @QueryParam("hasPublishedVersion")
    private boolean hasPublishedVersion;

    @ApiModelProperty("Наличие первичного ключа")
    @QueryParam("hasPrimaryAttribute")
    private boolean hasPrimaryAttribute;

    @ApiModelProperty("Код справочника")
    @QueryParam("displayCode")
    private String displayCode;

    @ApiModelProperty("Паспорт справочника")
    @QueryParam("passport")
    private Map<String, String> passport;

    public List<Integer> getRefBookIds() {
        return refBookIds;
    }

    public void setRefBookIds(List<Integer> refBookIds) {
        this.refBookIds = refBookIds;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getExcludeByVersionId() {
        return excludeByVersionId;
    }

    public void setExcludeByVersionId(Integer excludeByVersionId) {
        this.excludeByVersionId = excludeByVersionId;
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

    public RefBookSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(RefBookSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }

    public boolean getIsNotArchived() {
        return isNotArchived;
    }

    public void setIsNotArchived(boolean isNotArchived) {
        this.isNotArchived = isNotArchived;
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

    public String getDisplayCode() {
        return displayCode;
    }

    public void setDisplayCode(String displayCode) {
        this.displayCode = displayCode;
    }

    public Map<String, String> getPassport() {
        return passport;
    }

    public void setPassport(Map<String, String> passport) {
        this.passport = passport;
    }
}
