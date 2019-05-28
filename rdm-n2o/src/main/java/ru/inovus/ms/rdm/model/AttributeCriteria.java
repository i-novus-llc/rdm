package ru.inovus.ms.rdm.model;

import net.n2oapp.criteria.api.Criteria;

public class AttributeCriteria extends Criteria {

    // Атрибут версии справочника
    private String code;
    private Integer versionId;

    public AttributeCriteria() {
    }

    public AttributeCriteria(String code, Integer versionId) {
        this.code = code;
        this.versionId = versionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }
}
