package ru.i_novus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiParam;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import javax.ws.rs.QueryParam;
import java.util.*;

/**
 * Критерий поиска записей справочника.
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

    @ApiParam("Фильтр по всем полям")
    @QueryParam("commonFilter")
    private String commonFilter;

    @ApiParam("Хеши строк")
    @QueryParam("rowHashList")
    private List<String> rowHashList;

    @ApiParam("Системные идентификаторы строк")
    @QueryParam("rowSystemIds")
    private List<Long> rowSystemIds;

    public SearchDataCriteria() {
        // Nothing to do.
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

    public String getCommonFilter() {
        return commonFilter;
    }

    public void setCommonFilter(String commonFilter) {
        this.commonFilter = commonFilter;
    }

    public List<String> getRowHashList() {
        return rowHashList;
    }

    public void setRowHashList(List<String> rowHashList) {
        this.rowHashList = rowHashList;
    }

    public List<Long> getRowSystemIds() {
        return rowSystemIds;
    }

    public void setRowSystemIds(List<Long> rowSystemIds) {
        this.rowSystemIds = rowSystemIds;
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
        if (!super.equals(o)) return false;

        SearchDataCriteria that = (SearchDataCriteria) o;
        return Objects.equals(localeCode, that.localeCode) &&
                Objects.equals(attributeFilters, that.attributeFilters) &&
                Objects.equals(plainAttributeFilters, that.plainAttributeFilters) &&

                Objects.equals(commonFilter, that.commonFilter) &&
                Objects.equals(rowHashList, that.rowHashList) &&
                Objects.equals(rowSystemIds, that.rowSystemIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), localeCode, attributeFilters, plainAttributeFilters,
                commonFilter, rowHashList, rowSystemIds);
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
