package ru.i_novus.ms.rdm.api.model.version;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;

import javax.ws.rs.QueryParam;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Если fieldType = REFERENCE, то в value = Reference.getValue
 */
public class AttributeFilter {

    @QueryParam("attribute")
    private String attributeName;

    @QueryParam("value")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LocalDate.class, name = "DATE"),
            @JsonSubTypes.Type(value = BigInteger.class, name = "INTEGER"),
            @JsonSubTypes.Type(value = BigDecimal.class, name = "FLOAT")
    })
    private Serializable value;

    @QueryParam("type")
    private FieldType fieldType;

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

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
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
                Objects.equals(value, that.value) &&
                Objects.equals(fieldType, that.fieldType) &&
                (searchType == that.searchType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, value, fieldType, searchType);
    }
}
