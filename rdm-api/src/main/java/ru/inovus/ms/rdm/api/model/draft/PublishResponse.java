package ru.inovus.ms.rdm.api.model.draft;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;

@ApiModel(value = "Модель результата публикации черновика",
        description = "Набор выходных параметров публикации черновика")
public class PublishResponse implements Serializable {

    @ApiParam("Код опубликованного справочника")
    private String refBookCode;

    @ApiModelProperty(value = "Идентификатор опубликованной версии")
    private Integer newId;

    @ApiModelProperty(value = "Идентификатор предыдущего опубликованной версии")
    private Integer oldId;

    public String getRefBookCode() {
        return refBookCode;
    }

    public void setRefBookCode(String refBookCode) {
        this.refBookCode = refBookCode;
    }

    public Integer getNewId() {
        return newId;
    }

    public void setNewId(Integer newId) {
        this.newId = newId;
    }

    public Integer getOldId() {
        return oldId;
    }

    public void setOldId(Integer oldId) {
        this.oldId = oldId;
    }

    @Override
    public String toString() {
        return JsonUtil.getAsJson(this);
    }
}