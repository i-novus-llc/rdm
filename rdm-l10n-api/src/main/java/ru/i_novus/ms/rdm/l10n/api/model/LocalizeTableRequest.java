package ru.i_novus.ms.rdm.l10n.api.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

@ApiModel(value = "Модель локализации таблицы версии",
        description = "Набор входных параметров создания копии таблицы версии для локализации")
public class LocalizeTableRequest implements DraftChangeRequest {

    @ApiModelProperty("Значение оптимистической блокировки версии")
    private Integer optLockValue;

    @ApiModelProperty("Код локали")
    private String localeCode;

    public LocalizeTableRequest() {
    }

    public LocalizeTableRequest(Integer optLockValue, String localeCode) {

        this.optLockValue = optLockValue;
        this.localeCode = localeCode;
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

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}
