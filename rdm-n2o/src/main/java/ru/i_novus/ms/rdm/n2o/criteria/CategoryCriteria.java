package ru.i_novus.ms.rdm.n2o.criteria;

import jakarta.ws.rs.QueryParam;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if(!super.equals(o)) return false;

        CategoryCriteria that = (CategoryCriteria) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
