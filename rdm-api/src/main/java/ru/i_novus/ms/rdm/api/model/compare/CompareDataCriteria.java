package ru.i_novus.ms.rdm.api.model.compare;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import jakarta.ws.rs.QueryParam;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApiModel("Критерии сравнения данных версий справочника")
// NB: Rename to `CompareVersionDataCriteria` to don`t equals to vds `CompareDataCriteria`.
public class CompareDataCriteria extends CompareCriteria {

    @ApiParam("Множество фильтров по первичным полям")
    @QueryParam("primaryAttributesFilters")
    private Set<List<AttributeFilter>> primaryAttributesFilters;

    @ApiParam("Признак получения только количества записей")
    @QueryParam("countOnly")
    private Boolean countOnly;

    @ApiParam("Признак использования кеша")
    @QueryParam("useCached")
    private Boolean useCached = Boolean.TRUE;

    public CompareDataCriteria() {
        // Nothing to do.
    }

    public CompareDataCriteria(Integer oldVersionId, Integer newVersionId) {
        super(oldVersionId, newVersionId, null);
    }

    public CompareDataCriteria(CompareCriteria criteria) {
        super(criteria);
    }

    public CompareDataCriteria(CompareDataCriteria criteria) {

        super(criteria);

        this.primaryAttributesFilters = criteria.getPrimaryAttributesFilters();
        this.countOnly = criteria.getCountOnly();
        this.useCached = criteria.getUseCached();
    }

    public Set<List<AttributeFilter>> getPrimaryAttributesFilters() {
        return primaryAttributesFilters;
    }

    public void setPrimaryAttributesFilters(Set<List<AttributeFilter>> primaryAttributesFilters) {
        this.primaryAttributesFilters = primaryAttributesFilters;
    }

    public Boolean getCountOnly() {
        return countOnly;
    }

    public void setCountOnly(Boolean countOnly) {
        this.countOnly = countOnly;
    }

    public Boolean getUseCached() {
        return useCached;
    }

    public void setUseCached(Boolean useCached) {
        this.useCached = useCached;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CompareDataCriteria that = (CompareDataCriteria) o;
        return Objects.equals(primaryAttributesFilters, that.primaryAttributesFilters) &&
                Objects.equals(countOnly, that.countOnly) &&
                Objects.equals(useCached, that.useCached);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), primaryAttributesFilters, countOnly, useCached);
    }
}
