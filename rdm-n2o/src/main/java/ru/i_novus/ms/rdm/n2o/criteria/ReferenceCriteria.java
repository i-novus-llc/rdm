package ru.i_novus.ms.rdm.n2o.criteria;

import net.n2oapp.criteria.api.Criteria;

public class ReferenceCriteria extends Criteria {

    /** Идентификатор версии. */
    private Integer versionId;

    /** Код атрибута-ссылки. */
    private String reference;

    /** Ссылочное значение. */
    private String value;

    /** Отображаемое ссылочное значение. */
    private String displayValue;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }
}
