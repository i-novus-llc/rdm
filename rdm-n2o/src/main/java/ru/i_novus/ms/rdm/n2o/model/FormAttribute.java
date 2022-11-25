package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

/** Модель атрибута для UI. */
public class FormAttribute {

    // attribute:
    /** Код атрибута. */
    private String code;

    /** Наименование атрибута. */
    private String name;

    /** Тип атрибута. */
    private FieldType type;

    /** Наименование типа атрибута. */
    // Вместо FormAttributeType до создания FormAttributeRequest как с RefBook.
    private String typeName;

    /** Признак первичного атрибута. */
    private Boolean isPrimary;

    /** Признак переводимого атрибута. */
    private Boolean localizable;

    /** Описание атрибута. */
    private String description;

    // reference:
    /** Код справочника, на который ссылаются. */
    private String referenceCode;

    /** Выражение для вычисления отображаемого ссылочного значения. */
    private String displayExpression;

    //<editor-fold default-state="collapsed" desc="Пользовательские проверки">
    private Boolean required;
    private Boolean unique;

    private Integer plainSize;

    private Integer intPartSize;
    private Integer fracPartSize;

    private BigInteger minInteger;
    private BigInteger maxInteger;

    private BigDecimal minFloat;
    private BigDecimal maxFloat;

    private LocalDate minDate;
    private LocalDate maxDate;

    private String regExp;
    //</editor-fold> // Пользовательские проверки

    //<editor-fold default-state="collapsed" desc="Методы доступа">

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

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Boolean getIsPrimary() {
        return isPrimary != null && isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Boolean getLocalizable() {
        return localizable;
    }

    public boolean isLocalizable() {
        return localizable != null && localizable;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getDisplayExpression() {
        return displayExpression;
    }

    public void setDisplayExpression(String displayExpression) {
        this.displayExpression = displayExpression;
    }

    //<editor-fold default-state="collapsed" desc="Методы доступа: Пользовательские проверки">

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getUnique() {
        return unique;
    }

    public void setUnique(Boolean unique) {
        this.unique = unique;
    }

    public Integer getPlainSize() {
        return plainSize;
    }

    public void setPlainSize(Integer plainSize) {
        this.plainSize = plainSize;
    }

    public Integer getIntPartSize() {
        return intPartSize;
    }

    public void setIntPartSize(Integer intPartSize) {
        this.intPartSize = intPartSize;
    }

    public Integer getFracPartSize() {
        return fracPartSize;
    }

    public void setFracPartSize(Integer fracPartSize) {
        this.fracPartSize = fracPartSize;
    }

    public BigInteger getMinInteger() {
        return minInteger;
    }

    public void setMinInteger(BigInteger minInteger) {
        this.minInteger = minInteger;
    }

    public BigInteger getMaxInteger() {
        return maxInteger;
    }

    public void setMaxInteger(BigInteger maxInteger) {
        this.maxInteger = maxInteger;
    }

    public BigDecimal getMinFloat() {
        return minFloat;
    }

    public void setMinFloat(BigDecimal minFloat) {
        this.minFloat = minFloat;
    }

    public BigDecimal getMaxFloat() {
        return maxFloat;
    }

    public void setMaxFloat(BigDecimal maxFloat) {
        this.maxFloat = maxFloat;
    }

    public LocalDate getMinDate() {
        return minDate;
    }

    public void setMinDate(LocalDate minDate) {
        this.minDate = minDate;
    }

    public LocalDate getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(LocalDate maxDate) {
        this.maxDate = maxDate;
    }

    public String getRegExp() {
        return regExp;
    }

    public void setRegExp(String regExp) {
        this.regExp = regExp;
    }

    //</editor-fold> // Методы доступа: Пользовательские проверки

    //</editor-fold> // Методы доступа

    public boolean hasIsPrimary() {
        return getIsPrimary() != null && getIsPrimary();
    }

    public void setLocalizable(Boolean localizable) {
        this.localizable = localizable;
    }

    public boolean isReferenceType() {
        return FieldType.REFERENCE.equals(getType());
    }
}
