package ru.inovus.ms.rdm.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Set;

@ApiModel("Критерии сравнения данных версий справочника")
public class CompareDataCriteria extends AbstractCriteria {

    @ApiModelProperty("Идентификатор старой версии")
    @QueryParam("oldVersionId")
    private Integer oldVersionId;

    @ApiModelProperty("Идентификатор новой версии")
    @QueryParam("newVersionId")
    private Integer newVersionId;

    @ApiModelProperty("Множество значений первичных ключей")
    @QueryParam("primaryFieldsFilters")
    private Set<List<FieldValue>> primaryFieldsFilters;

    public CompareDataCriteria() {
    }

    public CompareDataCriteria(Integer oldVersionId, Integer newVersionId) {
        this.oldVersionId = oldVersionId;
        this.newVersionId = newVersionId;
    }

    public CompareDataCriteria(Integer oldVersionId, Integer newVersionId, Set<List<FieldValue>> primaryFieldsFilters) {
        this.oldVersionId = oldVersionId;
        this.newVersionId = newVersionId;
        this.primaryFieldsFilters = primaryFieldsFilters;
    }

    public Integer getOldVersionId() {
        return oldVersionId;
    }

    public void setOldVersionId(Integer oldVersionId) {
        this.oldVersionId = oldVersionId;
    }

    public Integer getNewVersionId() {
        return newVersionId;
    }

    public void setNewVersionId(Integer newVersionId) {
        this.newVersionId = newVersionId;
    }

    public Set<List<FieldValue>> getPrimaryFieldsFilters() {
        return primaryFieldsFilters;
    }

    public void setPrimaryFieldsFilters(Set<List<FieldValue>> primaryFieldsFilters) {
        this.primaryFieldsFilters = primaryFieldsFilters;
    }
}
