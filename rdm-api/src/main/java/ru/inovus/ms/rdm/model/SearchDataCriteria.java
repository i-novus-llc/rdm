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
}
