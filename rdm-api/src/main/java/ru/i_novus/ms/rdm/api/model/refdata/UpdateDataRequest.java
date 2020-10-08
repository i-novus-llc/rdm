package ru.i_novus.ms.rdm.api.model.refdata;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.Collections;
import java.util.List;

@ApiModel(value = "Модель изменения записей черновика",
        description = "Набор входных параметров для изменения записей черновика")
public class UpdateDataRequest implements DraftChangeRequest {

    @ApiModelProperty("Значение оптимистической блокировки версии-черновика")
    private Integer optLockValue;

    @ApiModelProperty("Добавляемые/обновляемые записи версии-черновика")
    private List<Row> rows;

    public UpdateDataRequest() {
    }

    public UpdateDataRequest(Integer optLockValue, List<Row> rows) {
        this.optLockValue = optLockValue;
        this.rows = rows;
    }

    public UpdateDataRequest(Integer optLockValue, Row row) {
        this(optLockValue, Collections.singletonList(row));
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
        return JsonUtil.toJsonString(this);
    }
}
