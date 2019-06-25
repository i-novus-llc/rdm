package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@ApiModel("Данные для публикации справочника")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UiRefBookPublish extends RefBook {

    @ApiModelProperty("Конфликтующие справочники")
    private Map<String, String> conflictReferrerNames;

    public UiRefBookPublish(RefBook refBook) {
        super(refBook);
    }

    public Map<String, String> getConflictReferrerNames() {
        return conflictReferrerNames;
    }

    public void setConflictReferrerNames(Map<String, String> conflictReferrerNames) {
        this.conflictReferrerNames = conflictReferrerNames;
    }
}
