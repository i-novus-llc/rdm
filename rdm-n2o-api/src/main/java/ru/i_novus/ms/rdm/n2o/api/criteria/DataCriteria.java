package ru.i_novus.ms.rdm.n2o.api.criteria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiParam;
import jakarta.ws.rs.QueryParam;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Критерий поиска списка записей справочника.
 * <p>
 * Created by znurgaliev on 14.11.2018.
 */
@SuppressWarnings("unused")
public class DataCriteria extends DataRecordCriteria {

    @ApiParam("Фильтр по атрибутам: код = значение")
    @QueryParam("filter")
    private Map<String, Serializable> filter;

    @ApiParam("Наличие конфликта данных")
    @QueryParam("hasDataConflict")
    private Boolean hasDataConflict;

    public DataCriteria() {
        super();
    }

    public DataCriteria(int pageNumber, int pageSize) {
        super(pageNumber, pageSize);
    }

    public DataCriteria(int pageNumber, int pageSize, Sort sort) {
        super(pageNumber, pageSize, sort);
    }

    public DataCriteria(DataCriteria criteria) {

        super(criteria);

        this.filter = criteria.getFilter();
        this.hasDataConflict = criteria.getHasDataConflict();
    }

    public Map<String, Serializable> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Serializable> filter) {
        this.filter = filter;
    }

    public Boolean getHasDataConflict() {
        return hasDataConflict;
    }

    public void setHasDataConflict(Boolean hasDataConflict) {
        this.hasDataConflict = hasDataConflict;
    }

    @JsonIgnore
    public boolean isHasDataConflict() {

        return Boolean.TRUE.equals(hasDataConflict) && (getLocaleCode() == null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if(!super.equals(o)) return false;

        DataCriteria that = (DataCriteria) o;
        return Objects.equals(filter, that.filter) &&
                Objects.equals(hasDataConflict, that.hasDataConflict);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), filter, hasDataConflict);
    }
}
