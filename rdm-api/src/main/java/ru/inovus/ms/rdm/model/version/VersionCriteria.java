package ru.inovus.ms.rdm.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

@ApiModel("Критерии поиска версий справочника")
public class VersionCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор справочника")
    @QueryParam("refBookId")
    private Integer refBookId;

    @ApiModelProperty("Исключить черновик")
    @QueryParam("excludeDraft")
    private Boolean excludeDraft;

    @ApiModelProperty("Номер версии")
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
