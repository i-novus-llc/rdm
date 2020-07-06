package ru.inovus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.util.Collections;
import java.util.List;

@ApiModel(value = "Модель изменения записей черновика",
        description = "Набор входных параметров для изменения записей черновика")
public class UpdateDataRequest implements DraftChangeRequest {

    @ApiModelProperty("Идентификатор версии-черновика")
    private Integer versionId;

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    @ApiModelProperty("Добавляемые/обновляемые записи версии-черновика")
    private List<Row> rows;

    public UpdateDataRequest() {
    }

    public UpdateDataRequest(Integer versionId, Integer optLockValue, List<Row> rows) {
        this.versionId = versionId;
        this.optLockValue = optLockValue;
        this.rows = rows;
    }

    public UpdateDataRequest(Integer versionId, Integer optLockValue, Row row) {
        this(versionId, optLockValue, Collections.singletonList(row));
    }

    @Override
    public Integer getVersionId() {
        return versionId;
    }

    @Override
    public void setVersionId(Integer draftId) {
        this.versionId = draftId;
    }

    @Override
    public Integer getOptLockValue() {
        return optLockValue;
    }

    @Override
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
