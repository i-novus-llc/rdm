package ru.i_novus.ms.rdm.n2o.criteria;

import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

/**
 * Критерий поиска категорий.
 */
public class CategoryCriteria extends AbstractCriteria {

    /** Наименование. */
    @QueryParam("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
