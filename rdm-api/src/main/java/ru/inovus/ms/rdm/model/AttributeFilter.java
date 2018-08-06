package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;

import javax.ws.rs.QueryParam;
import java.time.LocalDate;

public class AttributeFilter {

    @QueryParam("attribute")
    private String attributeName;

    @QueryParam("value")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LocalDate.class, name = "DATE"),
            @JsonSubTypes.Type(value = Reference.class, name = "REFERENCE")
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
        else
            switch (fieldType) {
                case STRING:
                    return SearchTypeEnum.LIKE;
                default:
                    return SearchTypeEnum.EXACT;
            }
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
}
