package ru.inovus.ms.rdm.model.version;

import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

public class VersionCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор справочника")
    @QueryParam("refBookId")
    private Integer refBookId;

    @ApiParam("Код справочника")
    @QueryParam("refBookCode")
    private String refBookCode;

    @ApiParam(value = "Исключение черновика", hidden = true)
    @QueryParam("excludeDraft")
    private Boolean excludeDraft;

    @ApiParam("Номер версии")
    @QueryParam("version")
    private String version;

    public VersionCriteria() {
        super();
    }

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

    public String getRefBookCode() {
        return refBookCode;
    }

    public void setRefBookCode(String refBookCode) {
        this.refBookCode = refBookCode;
    }

    public Boolean getExcludeDraft() {
        return excludeDraft != null && excludeDraft;
    }

    public void setExcludeDraft(Boolean excludeDraft) {
        this.excludeDraft = excludeDraft;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
