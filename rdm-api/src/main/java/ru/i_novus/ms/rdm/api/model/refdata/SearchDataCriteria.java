package ru.i_novus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiParam;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;

import javax.ws.rs.QueryParam;
import java.util.*;

/**
 * Критерий поиска данных справочника.
 */
public class SearchDataCriteria extends AbstractCriteria {

    @ApiParam("Код локали")
    @QueryParam("localeCode")
    private String localeCode;

    @ApiParam("Множество списков фильтров по отдельным полям")
    @QueryParam("attributeFilter")
    private Set<List<AttributeFilter>> attributeFilters;

    @ApiParam("Простые фильтры по полям")
    @QueryParam("plainAttributeFilter")
    private Map<String, String> plainAttributeFilters;

    @ApiParam("Системные идентификаторы строк")
    @QueryParam("rowSystemIds")
    private List<Long> rowSystemIds;

    @ApiParam("Фильтр по всем полям")
    @QueryParam("commonFilter")
    private String commonFilter;

    public SearchDataCriteria() {
    }

    public SearchDataCriteria(int pageNumber, int pageSize) {

        super(pageNumber, pageSize);
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public Set<List<AttributeFilter>> getAttributeFilters() {
        return attributeFilters;
    }

    public void setAttributeFilters(Set<List<AttributeFilter>> attributeFilters) {
        this.attributeFilters = attributeFilters;
    }

    public Map<String, String> getPlainAttributeFilters() {
        return plainAttributeFilters;
    }

    public void setPlainAttributeFilters(Map<String, String> plainAttributeFilters) {
        this.plainAttributeFilters = plainAttributeFilters;
    }

    public List<Long> getRowSystemIds() {
        return rowSystemIds;
    }

    public void setRowSystemIds(List<Long> rowSystemIds) {
        this.rowSystemIds = rowSystemIds;
    }

    public String getCommonFilter() {
        return commonFilter;
    }

    public void setCommonFilter(String commonFilter) {
        this.commonFilter = commonFilter;
    }

    public void addAttributeFilterList(List<AttributeFilter> attributeFilterList) {

        if (this.attributeFilters == null) {
            this.attributeFilters = new HashSet<>();
        }

        this.attributeFilters.add(attributeFilterList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchDataCriteria that = (SearchDataCriteria) o;

        if (!Objects.equals(attributeFilters, that.attributeFilters))
            return false;
        return Objects.equals(commonFilter, that.commonFilter);
    }

    @Override
    public int hashCode() {
        int result = attributeFilters != null ? attributeFilters.hashCode() : 0;
        result = 31 * result + (commonFilter != null ? commonFilter.hashCode() : 0);
        return result;
    }
}
