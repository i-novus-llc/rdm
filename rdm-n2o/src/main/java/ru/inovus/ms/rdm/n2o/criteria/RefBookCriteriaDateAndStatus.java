package ru.inovus.ms.rdm.n2o.criteria;

import ru.inovus.ms.rdm.n2o.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.n2o.model.RefBookStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

/**
 * Критерий поиска справочников с конвертацией даты.
 */
public class RefBookCriteriaDateAndStatus extends RefBookCriteria {

    public void setRefBookId(Integer refBookId) {
        super.setRefBookIds(Collections.singletonList(refBookId));
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
            case ARCHIVED:
                setIsArchived(true);
                break;

            case HAS_DRAFT:
                setHasDraft(true);
                break;

            case PUBLISHED:
                setHasPublished(true);
                break;
        }
    }

    private static LocalDateTime convertDateToLocalDateTime(Date date) {
        return (date == null) ? null : date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
