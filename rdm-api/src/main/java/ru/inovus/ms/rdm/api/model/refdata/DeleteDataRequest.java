package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.util.Collections;
import java.util.List;

@ApiModel(value = "Модель удаления записей черновика",
        description = "Набор входных параметров для удаления записей черновика")
public class DeleteDataRequest implements DraftChangeRequest {

    @ApiModelProperty(value = "Идентификатор черновика")
    private Integer draftId;

    @ApiParam("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    @ApiParam("Записи черновика")
    private List<Row> rows;

    public DeleteDataRequest() {
    }

    public DeleteDataRequest(Integer draftId, Integer optLockValue, List<Row> rows) {
        this.draftId = draftId;
        this.optLockValue = optLockValue;
        this.rows = rows;
    }

    public DeleteDataRequest(Integer draftId, Integer optLockValue, Row row) {
        this(draftId, optLockValue, Collections.singletonList(row));
    }

    public Integer getDraftId() {
        return draftId;
    }

    public void setDraftId(Integer draftId) {
        this.draftId = draftId;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
