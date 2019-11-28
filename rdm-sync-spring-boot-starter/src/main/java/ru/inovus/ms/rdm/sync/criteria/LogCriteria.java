package ru.inovus.ms.rdm.sync.criteria;

import lombok.Getter;
import lombok.Setter;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;
import java.time.LocalDate;

/**
 * @author lgalimova
 * @since 28.02.2019
 */
@Getter
@Setter
/**
 * Критерии поиска записи журнала
 */
public class LogCriteria extends AbstractCriteria {
    /**
     * Код справочника
     */
    @QueryParam("refbookCode")
    private String refbookCode;

    /**
     * Дата записи журнала
     */
    @QueryParam("date")
    @NotNull
    private LocalDate date;
}
