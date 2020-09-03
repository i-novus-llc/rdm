package ru.i_novus.ms.rdm.l10n.api.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.List;

@ApiModel(value = "Модель локализации записей версии",
        description = "Набор входных параметров для локализации записей версии")
public class LocalizeDataRequest implements DraftChangeRequest {

    @ApiModelProperty("Значение оптимистической блокировки версии")
    private Integer optLockValue;

    @ApiModelProperty("Код локали")
    private String localeCode;

    @ApiModelProperty("Локализуемые записи версии")
    private List<Row> rows;

    public LocalizeDataRequest() {
    }

    public LocalizeDataRequest(Integer optLockValue, String localeCode, List<Row> rows) {

        this.optLockValue = optLockValue;
        this.localeCode = localeCode;
        this.rows = rows;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
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
