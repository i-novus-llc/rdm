package ru.inovus.ms.rdm.sync.criteria;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

/**
 * @author lgalimova
 * @since 28.02.2019
 */
@Getter
@Setter
@ApiModel("Критерии поиска записи журнала")
public class LogCriteria extends AbstractCriteria {
    @ApiModelProperty("Код справочника")
    @QueryParam("refbookCode")
    private String refbookCode;

    @ApiModelProperty("Дата записи журнала")
    @QueryParam("date")
    @NotNull
    private String date;
}
