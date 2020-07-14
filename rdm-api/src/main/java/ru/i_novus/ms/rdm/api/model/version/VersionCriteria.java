package ru.i_novus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiParam;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

/**
 * Критерий поиска версий справочников.
 */
public class VersionCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор версии")
    @QueryParam("id")
    private Integer id;

    @ApiParam("Идентификатор справочника")
    @QueryParam("refBookId")
    private Integer refBookId;

    @ApiParam("Код справочника")
    @QueryParam("refBookCode")
    private String refBookCode;

    @ApiParam("Исключение черновика")
    @QueryParam("excludeDraft")
    private boolean excludeDraft;

    @ApiParam("Номер версии")
    @QueryParam("version")
    private String version;

    public VersionCriteria() {
        super();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public boolean getExcludeDraft() {
        return excludeDraft;
    }

    public void setExcludeDraft(boolean excludeDraft) {
        this.excludeDraft = excludeDraft;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
