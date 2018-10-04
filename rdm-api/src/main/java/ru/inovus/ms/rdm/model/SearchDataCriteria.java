package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Set;

@ApiModel("Критерии поиска данных справочника")
public class SearchDataCriteria extends AbstractCriteria{
    @ApiModelProperty("Фильтр по отдельным полям")
    @QueryParam("attributeFilter")
    private List<AttributeFilter> attributeFilter;

    @ApiModelProperty("Множество фильтров по отдельным полям")
    @QueryParam("primaryFieldsFilters")
    private Set<List<FieldValue>> primaryFieldsFilters;

    @ApiModelProperty("Фильтр по всем полям")
    @QueryParam("commonFilter")
    private String commonFilter;

    public SearchDataCriteria(List<AttributeFilter> attributeFilter, String commonFilter) {
        this.attributeFilter = attributeFilter;
        this.commonFilter = commonFilter;
    }

    public SearchDataCriteria(List<AttributeFilter> attributeFilter, Set<List<FieldValue>> primaryFieldsFilters, String commonFilter) {
        this(attributeFilter, commonFilter);
        this.primaryFieldsFilters = primaryFieldsFilters;
    }

    public SearchDataCriteria() {
    }

    public List<AttributeFilter> getAttributeFilter() {
        return attributeFilter;
    }

    public void setAttributeFilter(List<AttributeFilter> attributeFilter) {
        this.attributeFilter = attributeFilter;
    }

    public Set<List<FieldValue>> getPrimaryFieldsFilters() {
        return primaryFieldsFilters;
    }

    public void setPrimaryFieldsFilters(Set<List<FieldValue>> primaryFieldsFilters) {
        this.primaryFieldsFilters = primaryFieldsFilters;
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

        if (attributeFilter != null ? !attributeFilter.equals(that.attributeFilter) : that.attributeFilter != null) return false;
        if (primaryFieldsFilters != null ? !primaryFieldsFilters.equals(that.primaryFieldsFilters) : that.primaryFieldsFilters != null) return false;
        return commonFilter != null ? commonFilter.equals(that.commonFilter) : that.commonFilter == null;
    }

    @Override
    public int hashCode() {
        int result = attributeFilter != null ? attributeFilter.hashCode() : 0;
        result = 31 * result + (commonFilter != null ? commonFilter.hashCode() : 0);
        return result;
    }
}
