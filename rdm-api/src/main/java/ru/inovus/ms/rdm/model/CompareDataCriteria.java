package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Set;

@ApiModel("Критерии сравнения данных версий справочника")
public class CompareDataCriteria extends CompareCriteria {

    @ApiModelProperty("Множество значений первичных ключей")
    @QueryParam("primaryFieldsFilters")
    private Set<List<FieldValue>> primaryFieldsFilters;

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

    public Set<List<FieldValue>> getPrimaryFieldsFilters() {
        return primaryFieldsFilters;
    }

    public void setPrimaryFieldsFilters(Set<List<FieldValue>> primaryFieldsFilters) {
        this.primaryFieldsFilters = primaryFieldsFilters;
    }

    public Boolean getCountOnly() {
        return countOnly;
    }

    public void setCountOnly(Boolean countOnly) {
        this.countOnly = countOnly;
    }

}
