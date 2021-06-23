package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;
import ru.inovus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Критерий поиска данных справочника.
 */
public class SearchDataCriteria extends AbstractCriteria {

    @ApiParam("Множество фильтров по отдельным полям")
    @QueryParam("attributeFilter")
    private Set<List<AttributeFilter>> attributeFilter;

    @ApiParam("Простые фильтры по полям")
    @QueryParam("plainAttributeFilter")
    private Map<String, String> plainAttributeFilter;

    @ApiParam("Системные идентификаторы строк")
    @QueryParam("rowSystemIds")
    private List<Long> rowSystemIds;

    @ApiParam("Фильтр по всем полям")
    @QueryParam("commonFilter")
    private String commonFilter;

    public SearchDataCriteria() {
    }

    public SearchDataCriteria(Set<List<AttributeFilter>> attributeFilter, String commonFilter) {
        this.attributeFilter = attributeFilter;
        this.commonFilter = commonFilter;
    }

    public SearchDataCriteria(Set<List<AttributeFilter>> attributeFilter, Map<String, String> plainAttributeFilter, String commonFilter) {
        this.attributeFilter = attributeFilter;
        this.plainAttributeFilter = plainAttributeFilter;
        this.commonFilter = commonFilter;
    }

    public SearchDataCriteria(int pageNumber, int pageSize, Set<List<AttributeFilter>> attributeFilter) {
        this(attributeFilter, null);
        this.setPageNumber(pageNumber);
        this.setPageSize(pageSize);
    }

    public Map<String, String> getPlainAttributeFilter() {
        return plainAttributeFilter;
    }

    public void setPlainAttributeFilter(Map<String, String> plainAttributeFilter) {
        this.plainAttributeFilter = plainAttributeFilter;
    }

    public Set<List<AttributeFilter>> getAttributeFilter() {
        return attributeFilter;
    }

    public void setAttributeFilter(Set<List<AttributeFilter>> attributeFilter) {
        this.attributeFilter = attributeFilter;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchDataCriteria that = (SearchDataCriteria) o;

        if (!Objects.equals(attributeFilter, that.attributeFilter))
            return false;
        return Objects.equals(commonFilter, that.commonFilter);
    }

    @Override
    public int hashCode() {
        int result = attributeFilter != null ? attributeFilter.hashCode() : 0;
        result = 31 * result + (commonFilter != null ? commonFilter.hashCode() : 0);
        return result;
    }
}
