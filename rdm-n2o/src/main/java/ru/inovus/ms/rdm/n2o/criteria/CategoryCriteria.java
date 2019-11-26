package ru.inovus.ms.rdm.n2o.criteria;


import net.n2oapp.criteria.api.Criteria;

public class CategoryCriteria extends Criteria {
    private String name;

    public CategoryCriteria() {
    }

    public CategoryCriteria(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
