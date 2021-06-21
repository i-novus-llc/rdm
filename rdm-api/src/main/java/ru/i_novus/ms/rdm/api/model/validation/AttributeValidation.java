package ru.i_novus.ms.rdm.api.model.validation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * Модель пользовательской проверки атрибута.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RequiredAttributeValidation.class, name = "REQUIRED"),
        @JsonSubTypes.Type(value = UniqueAttributeValidation.class, name = "UNIQUE"),
        @JsonSubTypes.Type(value = PlainSizeAttributeValidation.class, name = "PLAIN_SIZE"),
        @JsonSubTypes.Type(value = FloatSizeAttributeValidation.class, name = "FLOAT_SIZE"),
        @JsonSubTypes.Type(value = IntRangeAttributeValidation.class, name = "INT_RANGE"),
        @JsonSubTypes.Type(value = FloatRangeAttributeValidation.class, name = "FLOAT_RANGE"),
        @JsonSubTypes.Type(value = DateRangeAttributeValidation.class, name = "DATE_RANGE"),
        @JsonSubTypes.Type(value = RegExpAttributeValidation.class, name = "REG_EXP")
})
public abstract class AttributeValidation implements Serializable {

    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Integer versionId;

    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String attribute;

    private AttributeValidationType type;

    public AttributeValidation(AttributeValidationType type) {
        this.type = type;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public AttributeValidationType getType() {
        return type;
    }

    public void setType(AttributeValidationType type) {
        this.type = type;
    }

    /**
     * Преобразование значений проверки в строку.
     *
     * @return Значения проверки в виде строки, формат зависит от реализации
     */
    public abstract String valuesToString();

    /**
     * Заполнение значений проверки из строки.
     *
     * @param value значения проверки в виде строки, формат зависит от реализации
     * @throws IllegalArgumentException при некорректном формате
     */
    public abstract AttributeValidation valueFromString(String value);

    public static AttributeValidation of(String stype, String val) {
        return ofTypeWithAttr(stype, val, null);
    }

    public static AttributeValidation ofTypeWithAttr(String stype, String val, String attr) {

        AttributeValidationType type = AttributeValidationType.valueOf(stype.toUpperCase());
        AttributeValidation validation = type.getValidationInstance().valueFromString(val);
        validation.setAttribute(attr);

        return validation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeValidation that = (AttributeValidation) o;
        return Objects.equals(versionId, that.versionId) &&
                Objects.equals(attribute, that.attribute) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId, attribute, type);
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder("AttributeValidation{");
        sb.append("versionId=").append(versionId);
        sb.append(", attribute='").append(attribute).append('\'');
        sb.append(", type=").append(type);
        sb.append(", values='").append(valuesToString()).append('\'');
        sb.append('}');

        return sb.toString();
    }
}
