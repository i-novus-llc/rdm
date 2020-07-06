package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.util.Collections;
import java.util.List;

@ApiModel(value = "Модель удаления записей черновика",
        description = "Набор входных параметров для удаления записей черновика")
public class DeleteDataRequest implements DraftChangeRequest {

    @ApiModelProperty("Идентификатор версии-черновика")
    private Integer versionId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    @ApiModelProperty("Удаляемые записи версии-черновика")
    private List<Row> rows;

    public DeleteDataRequest() {
    }

    public DeleteDataRequest(Integer versionId, Integer optLockValue, List<Row> rows) {
        this.versionId = versionId;
        this.optLockValue = optLockValue;
        this.rows = rows;
    }

    public DeleteDataRequest(Integer versionId, Integer optLockValue, Row row) {
        this(versionId, optLockValue, Collections.singletonList(row));
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer draftId) {
        this.versionId = draftId;
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
