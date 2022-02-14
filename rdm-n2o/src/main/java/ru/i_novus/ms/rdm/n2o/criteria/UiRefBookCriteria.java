package ru.i_novus.ms.rdm.n2o.criteria;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.n2o.model.RefBookStatus;

import static java.util.Collections.singletonList;

/**
 * Критерий поиска справочников для UI.
 */
public class UiRefBookCriteria extends RefBookCriteria {

    public void setRefBookId(Integer refBookId) {
        super.setRefBookIds(singletonList(refBookId));
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
}
