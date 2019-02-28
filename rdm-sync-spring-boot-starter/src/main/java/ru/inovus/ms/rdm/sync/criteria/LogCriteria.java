package ru.inovus.ms.rdm.sync.criteria;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import ru.inovus.ms.rdm.model.AbstractCriteria;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;
import java.time.LocalDate;

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
    private LocalDate date;
}
