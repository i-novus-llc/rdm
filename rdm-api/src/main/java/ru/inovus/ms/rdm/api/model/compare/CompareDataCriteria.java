package ru.inovus.ms.rdm.api.model.compare;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApiModel("Критерии сравнения данных версий справочника")
// NB: Rename to `CompareVersionDataCriteria` to don`t equals to vds `CompareDataCriteria`.
public class CompareDataCriteria extends CompareCriteria {

    @ApiModelProperty("Множество фильтров по первичным полям")
    @QueryParam("primaryAttributesFilters")
    private Set<List<AttributeFilter>> primaryAttributesFilters;

    @ApiModelProperty("Флаг для получения только количества записей")
    @QueryParam("countOnly")
    private Boolean countOnly;

    public CompareDataCriteria() {
    }

    public CompareDataCriteria(Integer oldVersionId, Integer newVersionId) {
        super(oldVersionId, newVersionId, null);
    }

    public CompareDataCriteria(CompareCriteria criteria) {
        super(criteria.getOldVersionId(), criteria.getNewVersionId(), criteria.getDiffStatus());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CompareDataCriteria that = (CompareDataCriteria) o;
        return Objects.equals(primaryAttributesFilters, that.primaryAttributesFilters) &&
                Objects.equals(countOnly, that.countOnly);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), primaryAttributesFilters, countOnly);
    }
}
