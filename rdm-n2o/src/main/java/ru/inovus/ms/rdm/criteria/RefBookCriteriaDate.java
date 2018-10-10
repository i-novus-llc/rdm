package ru.inovus.ms.rdm.criteria;

import io.swagger.annotations.ApiModel;
import ru.inovus.ms.rdm.model.RefBookCriteria;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApiModel("Критерии поиска справочника с конвертацией даты")
public class RefBookCriteriaDate extends RefBookCriteria {

    public void setFromDateBegin(Date fromDateBegin) {
        super.setFromDateBegin(convertDateToLocalDateTime(fromDateBegin));
    }

    public void setFromDateEnd(Date fromDateEnd) {
        super.setFromDateEnd(convertDateToLocalDateTime(fromDateEnd));
    }

    private static LocalDateTime convertDateToLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
