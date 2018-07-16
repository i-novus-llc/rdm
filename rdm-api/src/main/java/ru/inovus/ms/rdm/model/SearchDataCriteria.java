package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;

import javax.ws.rs.QueryParam;
import java.util.List;

@ApiModel("Критерии поиска данных справочника")
public class SearchDataCriteria {
    @ApiModelProperty("Фильтр по отдельным полям")
    @QueryParam("fieldFilter")
    private List<FieldSearchCriteria> fieldFilter;

    @ApiModelProperty("Фильтр по всем полям")
    @QueryParam("excludeDraft")
    private String commonFilter;

    public SearchDataCriteria(List<FieldSearchCriteria> fieldFilter, String commonFilter) {
        this.fieldFilter = fieldFilter;
        this.commonFilter = commonFilter;
    }

    public SearchDataCriteria() {
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchDataCriteria that = (SearchDataCriteria) o;

        if (fieldFilter != null ? !fieldFilter.equals(that.fieldFilter) : that.fieldFilter != null) return false;
        return commonFilter != null ? commonFilter.equals(that.commonFilter) : that.commonFilter == null;
    }

    @Override
    public int hashCode() {
        int result = fieldFilter != null ? fieldFilter.hashCode() : 0;
        result = 31 * result + (commonFilter != null ? commonFilter.hashCode() : 0);
        return result;
    }
}
