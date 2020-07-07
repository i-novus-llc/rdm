package ru.inovus.ms.rdm.api.model.refbook;

import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Критерий поиска справочников. */
@SuppressWarnings("unused")
public class RefBookCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор справочника")
    @QueryParam("refBookId")
    private List<Integer> refBookIds;

    @ApiParam("Код справочника (по вхождению без учета регистра)")
    @QueryParam("code")
    private String code;

    @ApiParam("Код справочника (по точному совпадению с учетом регистра). Будет использован только в случае, если не задан поиск по вхождению без учета регистра.")
    @QueryParam("codeExact")
    private String codeExact;

    @ApiParam("Версия справочника")
    @QueryParam("versionId")
    private Integer versionId;

    @ApiParam(value = "Версия для исключения справочника", hidden = true)
    @QueryParam("excludeByVersionId")
    private Integer excludeByVersionId;

    @ApiParam("Дата начала для последней публикации")
    @QueryParam("fromDateBegin")
    private LocalDateTime fromDateBegin;

    @ApiParam("Дата конца для последней публикации")
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

    @ApiParam(value = "Не в архиве", hidden = true)
    @QueryParam("nonArchived")
    private boolean nonArchived;

    @ApiParam("Наличие черновика")
    @QueryParam("hasDraft")
    private boolean hasDraft;

    @ApiParam("Опубликованность справочника")
    @QueryParam("hasPublished")
    private boolean hasPublished;

    @ApiParam(value = "Получение версий справочников", hidden = true)
    @QueryParam("includeVersions")
    private boolean includeVersions;

    @ApiParam("Исключение черновика")
    @QueryParam("excludeDraft")
    private boolean excludeDraft;

    @ApiParam(value = "Наличие первичного ключа", hidden = true)
    @QueryParam("hasPrimaryAttribute")
    private boolean hasPrimaryAttribute;

    @ApiParam(value = "Отображаемый код справочника", hidden = true)
    @QueryParam("displayCode")
    private String displayCode;

    @ApiParam(value = "Паспорт справочника", hidden = true)
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

    public String getCodeExact() {
        return codeExact;
    }

    public void setCodeExact(String codeExact) {
        this.codeExact = codeExact;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
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
