package ru.inovus.ms.rdm.n2o.model;

import io.swagger.annotations.ApiModel;
import net.n2oapp.criteria.api.Criteria;

@ApiModel("Критерии поиска атрибута справочника")
public class AttributeCriteria extends Criteria {

    // Атрибут версии справочника
    private String code;

    private String name;

    private Integer versionId;

    @SuppressWarnings("unused")
    public AttributeCriteria() {
    }

    public AttributeCriteria(Integer versionId) {
        this.versionId = versionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }
}
