package ru.inovus.ms.rdm.model;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CommonField.class, name = "BooleanField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "DateField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "FloatField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "IntegerField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "IntegerStringField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "ListField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "ReferenceField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "StringField"),
        @JsonSubTypes.Type(value = CommonField.class, name = "TreeField")
})
public class FieldMixin {}
