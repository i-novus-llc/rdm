package ru.i_novus.ms.rdm.n2o.criteria;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.n2o.model.RefBookStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static java.util.Collections.singletonList;

/**
 * Критерий поиска справочников с конвертацией даты.
 */
public class RefBookCriteriaDateAndStatus extends RefBookCriteria {

    public void setRefBookId(Integer refBookId) {
        super.setRefBookIds(singletonList(refBookId));
    }

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

        switch (status) {
            case ARCHIVED -> setIsArchived(true);
            case HAS_DRAFT -> setHasDraft(true);
            case PUBLISHED -> setHasPublished(true);
        }
    }

    private static LocalDateTime convertDateToLocalDateTime(Date date) {

        return (date == null) ? null : date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
