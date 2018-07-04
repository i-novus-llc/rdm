package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;

import javax.ws.rs.QueryParam;
import java.util.List;

@ApiModel("Критерии поиска версий справочника")
public class VersionCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор справочника")
    @QueryParam("refBookId")
    private Integer refBookId;

    @ApiModelProperty("Исключить черновик")
    @QueryParam("excludeDraft")
    private Boolean excludeDraft;

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
}
