package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;

import javax.ws.rs.QueryParam;
import java.time.LocalDateTime;

@ApiModel("Критерии поиска справочника")
public class RefBookCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор справочника")
    @QueryParam("refBookId")
    private Integer refBookId;

    @ApiModelProperty("Код")
    @QueryParam("code")
    private String code;

    @ApiModelProperty("Дата последней публикации")
    @QueryParam("fromDateBegin")
    private LocalDateTime fromDateBegin;

    @ApiModelProperty("Дата последней публикации")
    @QueryParam("fromDateEnd")
    private LocalDateTime fromDateEnd;

    @ApiModelProperty("Статус справочника")
    @QueryParam("status.id")
    private RefBookStatus status;

    @ApiModelProperty("Паспорт справочника")
    @QueryParam("passport")
    private Passport passport;

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

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

    public RefBookStatus getStatus() {
        return status;
    }

    public void setStatus(RefBookStatus status) {
        this.status = status;
    }

    public Passport getPassport() {
        return passport;
    }

    public void setPassport(Passport passport) {
        this.passport = passport;
    }
}
