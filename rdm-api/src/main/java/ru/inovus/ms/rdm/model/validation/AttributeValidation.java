package ru.inovus.ms.rdm.model.validation;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;

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

    @ApiModelProperty(readOnly = true)
    private Integer versionId;
    @ApiModelProperty(readOnly = true)
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
     * @return Значения проверки в виде строки, формат зависит от реализации
     */
    public abstract String valuesToString();

    /**
     * Заполнение значений проверки из строки
     * @param value значение(я) проверки в виде String, формат зависит от реализации
     * @throws IllegalArgumentException если некорректный формат
     */
    public abstract AttributeValidation valueFromString(String value);
}
