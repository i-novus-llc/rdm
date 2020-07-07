package ru.inovus.ms.rdm.n2o.model;

import io.swagger.annotations.ApiModel;
import net.n2oapp.criteria.api.Criteria;

@ApiModel("Критерии поиска атрибутов справочника")
public class AttributeCriteria extends Criteria {

    /** Код атрибута версии справочника. */
    private String code;

    /** Наименование атрибута версии справочника. */
    private String name;

    /** Идентификатор версии справочника. */
    private Integer versionId;

    /** Значение оптимистической блокировки версии. */
    private Integer optLockValue;

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

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }
}
