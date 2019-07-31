package ru.inovus.ms.rdm.model.refbook;

import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Критерий поиска справочников.
 */
public class RefBookCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор справочника")
    @QueryParam("refBookId")
    private List<Integer> refBookIds;

    @ApiParam("Код")
    @QueryParam("code")
    private String code;

    @ApiParam("Получение версий справочников")
    @QueryParam("includeVersions")
    private boolean includeVersions;

    @ApiParam("Исключение черновика")
    @QueryParam("excludeDraft")
    private boolean excludeDraft;

    @ApiParam("Версия для исключения справочника")
    @QueryParam("excludeByVersionId")
    private Integer excludeByVersionId;

    @ApiParam("Дата последней публикации")
    @QueryParam("fromDateBegin")
    private LocalDateTime fromDateBegin;

    @ApiParam("Дата последней публикации")
    @QueryParam("fromDateEnd")
    private LocalDateTime fromDateEnd;

    @ApiParam("Тип источника данных")
    @QueryParam("sourceType")
    private RefBookSourceType sourceType;

    @ApiParam("Категория")
    @QueryParam("category")
    private String category;

    @ApiParam("В архиве")
    @QueryParam("isArchived")
    private boolean isArchived;

    @ApiParam("Не в архиве")
    @QueryParam("nonArchived")
    private boolean nonArchived;

    @ApiParam("Опубликованность справочника")
    @QueryParam("hasPublished")
    private boolean hasPublished;

    @ApiParam("Наличие черновика")
    @QueryParam("hasDraft")
    private boolean hasDraft;

    // NB: По смыслу совпадает с hasPublished, хотя и отличается предикатом.
    // NB: may-be: Оставить только этот вариант, использовав предикат для hasPublished.
    @ApiParam("Наличие опубликованной версии")
    @QueryParam("hasPublishedVersion")
    private boolean hasPublishedVersion;

    @ApiParam(value = "Наличие первичного ключа", hidden = true)
    @QueryParam("hasPrimaryAttribute")
    private boolean hasPrimaryAttribute;

    @ApiParam("Паспорт справочника")
    @QueryParam("passport")
    private Map<String, String> passport;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean getIncludeVersions() {
        return includeVersions;
    }

    public void setIncludeVersions(boolean includeVersions) {
        this.includeVersions = includeVersions;
    }

    public boolean getExcludeDraft() {
        return excludeDraft;
    }

    public void setExcludeDraft(boolean excludeDraft) {
        this.excludeDraft = excludeDraft;
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

    public boolean getNonArchived() {
        return nonArchived;
    }

    public void setNonArchived(boolean nonArchived) {
        this.nonArchived = nonArchived;
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
