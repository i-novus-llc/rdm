package ru.inovus.ms.rdm.model.validation;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RequiredValidationValue.class, name = "REQUIRED"),
        @JsonSubTypes.Type(value = UniqueValidationValue.class, name = "UNIQUE"),
        @JsonSubTypes.Type(value = PlainSizeValidationValue.class, name = "PLAIN_SIZE"),
        @JsonSubTypes.Type(value = FloatSizeValidationValue.class, name = "FLOAT_SIZE"),
        @JsonSubTypes.Type(value = IntRangeValidationValue.class, name = "INT_RANGE"),
        @JsonSubTypes.Type(value = FloatRangeValidationValue.class, name = "FLOAT_RANGE"),
        @JsonSubTypes.Type(value = DateRangeValidationValue.class, name = "DATE_RANGE"),
        @JsonSubTypes.Type(value = RegExpValidationValue.class, name = "REG_EXP")
})
@ApiModel(subTypes = {PlainSizeValidationValue.class})
public abstract class AttributeValidationValue {

    @ApiModelProperty(readOnly = true)
    private Integer versionId;
    @ApiModelProperty(readOnly = true)
    private String attribute;
    private AttributeValidationType type;

    public AttributeValidationValue(AttributeValidationType type) {
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
    public abstract AttributeValidationValue valueFromString(String value);
}
