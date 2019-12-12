package ru.inovus.ms.rdm.api.model.validation;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

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
public abstract class AttributeValidation {

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

    public AttributeValidation setVersionId(Integer versionId) {
        this.versionId = versionId;
        return this;
    }

    public String getAttribute() {
        return attribute;
    }

    public AttributeValidation setAttribute(String attribute) {
        this.attribute = attribute;
        return this;
    }

    public AttributeValidationType getType() {
        return type;
    }

    public AttributeValidation setType(AttributeValidationType type) {
        this.type = type;
        return this;
    }

    /**
     * @return Значения проверки в виде строки, формат зависит от реализации
     */
    public abstract String valuesToString();

    /**
     * Заполнение значений проверки из строки
     * @param value значение(я) проверки в виде String, формат зависит от реализации
     * @throws IllegalArgumentException если некорректный формат
     */
    public abstract AttributeValidation valueFromString(String value);

    public static AttributeValidation of(String stype, String val) {
        AttributeValidationType type = AttributeValidationType.valueOf(stype.toUpperCase());
        return type.getValidationInstance().valueFromString(val);
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
}
