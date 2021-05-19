package ru.i_novus.ms.rdm.n2o.criteria;

import net.n2oapp.criteria.api.Criteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

/**
 * Критерий поиска типов справочника.
 */
public class RefBookTypeCriteria extends Criteria {

    /** Идентификатор (код). */
    private RefBookTypeEnum id;

    /** Наименование. */
    private String name;

    public RefBookTypeEnum getId() {
        return id;
    }

    public void setId(RefBookTypeEnum id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
