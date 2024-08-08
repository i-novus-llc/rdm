package ru.i_novus.ms.rdm.api.model.version;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.ws.rs.QueryParam;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Фильтр по атрибуту.
 * <p/>
 * Если fieldType = REFERENCE, то в value = Reference.getValue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeFilter implements Serializable {

    @QueryParam("attribute")
    private String attributeName;

    @QueryParam("fieldType")
    private FieldType fieldType;

    @QueryParam("value")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "fieldType", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = String.class, name = "STRING"),
            @JsonSubTypes.Type(value = BigInteger.class, name = "INTEGER"),
            @JsonSubTypes.Type(value = BigDecimal.class, name = "FLOAT"),
            @JsonSubTypes.Type(value = LocalDate.class, name = "DATE"),
            @JsonSubTypes.Type(value = Boolean.class, name = "BOOLEAN"),
            @JsonSubTypes.Type(value = String.class, name = "REFERENCE")
    })
    private Serializable value;

    @QueryParam("searchType")
    private SearchTypeEnum searchType;

    public AttributeFilter() {
    }

    public AttributeFilter(String attributeName, Serializable value, FieldType fieldType) {

        this.attributeName = attributeName;
        this.value = value;
        this.fieldType = fieldType;
    }

    public AttributeFilter(String attributeName, Serializable value, FieldType fieldType, SearchTypeEnum searchTypeEnum) {

        this(attributeName, value, fieldType);

        this.searchType = searchTypeEnum;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    public SearchTypeEnum getSearchType() {

        if (searchType != null)
            return searchType;

        if (FieldType.STRING.equals(fieldType))
            return SearchTypeEnum.LIKE;

        return SearchTypeEnum.EXACT;
    }

    public void setSearchType(SearchTypeEnum searchType) {
        this.searchType = searchType;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeFilter that = (AttributeFilter) o;
        return Objects.equals(attributeName, that.attributeName) &&
                (fieldType == that.fieldType) &&
                Objects.equals(value, that.value) &&
                (searchType == that.searchType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, fieldType, value, searchType);
    }
}
