package ru.inovus.ms.rdm.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.enumeration.RefBookStatusType;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

@ApiModel("Критерии поиска версий справочника со ссылками")
public class ReferrerVersionCriteria extends AbstractCriteria {

    @ApiModelProperty("Код справочника, на который ссылаются")
    @QueryParam("refBookCode")
    private String refBookCode;

    @ApiModelProperty("Тип статуса справочника")
    @QueryParam("statusType")
    private RefBookStatusType statusType;

    @ApiModelProperty("Тип источника данных")
    @QueryParam("sourceType")
    private RefBookSourceType sourceType;

    @SuppressWarnings("unused")
    public ReferrerVersionCriteria() {
    }

    public ReferrerVersionCriteria(String refBookCode, RefBookStatusType statusType, RefBookSourceType sourceType) {
        this.refBookCode = refBookCode;
        this.statusType = statusType;
        this.sourceType = sourceType;
    }

    public String getRefBookCode() {
        return refBookCode;
    }

    public void setRefBookCode(String refBookCode) {
        this.refBookCode = refBookCode;
    }

    public RefBookStatusType getStatusType() {
        return statusType;
    }

    public void setStatusType(RefBookStatusType statusType) {
        this.statusType = statusType;
    }

    public RefBookSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(RefBookSourceType sourceType) {
        this.sourceType = sourceType;
    }
}
