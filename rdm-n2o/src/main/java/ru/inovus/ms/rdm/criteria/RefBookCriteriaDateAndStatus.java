package ru.inovus.ms.rdm.criteria;

import io.swagger.annotations.ApiModel;
import ru.inovus.ms.rdm.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.model.RefBookStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

@ApiModel("Критерии поиска справочника с конвертацией даты")
public class RefBookCriteriaDateAndStatus extends RefBookCriteria {

    public void setFromDateBegin(Date fromDateBegin) {
        super.setFromDateBegin(convertDateToLocalDateTime(fromDateBegin));
    }

    public void setFromDateEnd(Date fromDateEnd) {
        super.setFromDateEnd(convertDateToLocalDateTime(fromDateEnd));
    }

    public void setStatus(RefBookStatus status) {
        setIsArchived(false);
        setHasDraft(false);
        setHasPublished(false);
        if (RefBookStatus.ARCHIVED.equals(status))
            setIsArchived(true);
        else if (RefBookStatus.HAS_DRAFT.equals(status))
            setHasDraft(true);
        else if (RefBookStatus.PUBLISHED.equals(status))
            setHasPublished(true);
    }

    private static LocalDateTime convertDateToLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public void setRefbookId(Integer refBookId) {
        super.setRefBookIds(Collections.singletonList(refBookId));
    }
}
