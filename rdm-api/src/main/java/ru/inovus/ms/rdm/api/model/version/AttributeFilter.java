package ru.inovus.ms.rdm.api.model.version;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;

import javax.ws.rs.QueryParam;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

/**
 * Если fieldType REFERENCE то в value = Reference.getValue
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
    private Object value;

    @QueryParam("type")
    private FieldType fieldType;

    @QueryParam("searchType")
    private SearchTypeEnum searchType;

    public AttributeFilter() {
    }

    public AttributeFilter(String attributeName, Object value, FieldType fieldType) {
        this.attributeName = attributeName;
        this.value = value;
        this.fieldType = fieldType;
    }

    public AttributeFilter(String attributeName, Object value, FieldType fieldType, SearchTypeEnum searchTypeEnum) {
        this.attributeName = attributeName;
        this.value = value;
        this.fieldType = fieldType;
        this.searchType = searchTypeEnum;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }


    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public SearchTypeEnum getSearchType() {
        if (searchType != null)
            return searchType;
        else if (FieldType.STRING.equals(fieldType))
            return SearchTypeEnum.LIKE;
        else
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

        if (attributeName != null ? !attributeName.equals(that.attributeName) : that.attributeName != null)
            return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (fieldType != that.fieldType) return false;
        return searchType == that.searchType;

    }

    @Override
    public int hashCode() {
        int result = attributeName != null ? attributeName.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (fieldType != null ? fieldType.hashCode() : 0);
        result = 31 * result + (searchType != null ? searchType.hashCode() : 0);
        return result;
    }
}
