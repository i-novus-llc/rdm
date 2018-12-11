package ru.inovus.ms.rdm.criteria;

import io.swagger.annotations.ApiModel;
import ru.inovus.ms.rdm.model.audit.AuditLogCriteria;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApiModel("Критерии поиска справочника с конвертацией даты")
public class AuditCriteriaDate extends AuditLogCriteria {

    public void setFromDate(Date fromDate) {
        super.setFromDate(convertDateToLocalDateTime(fromDate));
    }

    public void setToDate(Date toDate) {
        super.setToDate(convertDateToLocalDateTime(toDate));
    }

    private static LocalDateTime convertDateToLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
