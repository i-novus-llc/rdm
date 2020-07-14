package ru.i_novus.ms.rdm.n2o.criteria;

import io.swagger.annotations.ApiParam;
import net.n2oapp.criteria.api.Criteria;

import javax.ws.rs.QueryParam;

/**
 * Критерий поиска статусов справочников.
 */
public class RefBookStatusCriteria extends Criteria {

    @ApiParam("Не в архиве")
    @QueryParam("nonArchived")
    private boolean nonArchived;

    @ApiParam("Исключение черновика")
    @QueryParam("excludeDraft")
    private boolean excludeDraft;

    public boolean getNonArchived() {
        return nonArchived;
    }

    public void setNonArchived(boolean nonArchived) {
        this.nonArchived = nonArchived;
    }

    public boolean getExcludeDraft() {
        return excludeDraft;
    }

    public void setExcludeDraft(boolean excludeDraft) {
        this.excludeDraft = excludeDraft;
    }
}
