package ru.inovus.ms.rdm.n2o.model;

/** Модель атрибута для передачи в UI. */
public class ReadAttribute extends FormAttribute {

    // refBook:
    /** Идентификатор версии справочника. */
    private Integer versionId;

    /** Значение оптимистической блокировки версии. */
    private Integer optLockValue;

    /** Признак связанного справочника (т.е. сам - ссылающийся). */
    private Boolean isReferrer;

    /** Наличие связанного справочника (т.е. ссылающегося на данный). */
    private Boolean hasReferrer;

    // attribute:
    /** Строка подстановки кода атрибута в displayExpression ссылки. */
    private String codeExpression;

    // reference:
    /** Идентификатор справочника, на который ссылаются. */
    private Integer referenceRefBookId;

    /** Тип отображения dipslayExpression ссылки: 1 - выбор одного атрибута, 2 - строка-значение. */
    private Integer displayType;

    /** Код отображаемого атрибута при displayType = 1. */
    private String displayAttribute;

    /** Наименование отображаемого атрибута при displayType = 1. */
    private String displayAttributeName;

    // conflict:
    /** Наличие конфликта структуры (для подсветки). */
    private Boolean hasStructureConflict;

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

    public Boolean getIsReferrer() {
        return isReferrer;
    }

    public void setIsReferrer(Boolean isReferrer) {
        this.isReferrer = isReferrer;
    }

    public Boolean getHasReferrer() {
        return hasReferrer;
    }

    public void setHasReferrer(Boolean hasReferrer) {
        this.hasReferrer = hasReferrer;
    }

    public String getCodeExpression() {
        return codeExpression;
    }

    public void setCodeExpression(String codeExpression) {
        this.codeExpression = codeExpression;
    }

    public Integer getReferenceRefBookId() {
        return referenceRefBookId;
    }

    public void setReferenceRefBookId(Integer referenceRefBookId) {
        this.referenceRefBookId = referenceRefBookId;
    }

    public Integer getDisplayType() {
        return displayType;
    }

    public void setDisplayType(Integer displayType) {
        this.displayType = displayType;
    }

    public String getDisplayAttribute() {
        return displayAttribute;
    }

    public void setDisplayAttribute(String displayAttribute) {
        this.displayAttribute = displayAttribute;
    }

    public String getDisplayAttributeName() {
        return displayAttributeName;
    }

    public void setDisplayAttributeName(String displayAttributeName) {
        this.displayAttributeName = displayAttributeName;
    }

    public Boolean getHasStructureConflict() {
        return hasStructureConflict;
    }

    public void setHasStructureConflict(Boolean hasStructureConflict) {
        this.hasStructureConflict = hasStructureConflict;
    }
}
