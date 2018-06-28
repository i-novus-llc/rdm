package ru.inovus.ms.rdm.model;

import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;

import java.util.List;

public class DraftCriteria {
    private List<FieldSearchCriteria> fieldFilter;
    private String commonFilter;

    public List<FieldSearchCriteria> getFieldFilter() {
        return fieldFilter;
    }

    public void setFieldFilter(List<FieldSearchCriteria> fieldFilter) {
        this.fieldFilter = fieldFilter;
    }

    public String getCommonFilter() {
        return commonFilter;
    }

    public void setCommonFilter(String commonFilter) {
        this.commonFilter = commonFilter;
    }
}
