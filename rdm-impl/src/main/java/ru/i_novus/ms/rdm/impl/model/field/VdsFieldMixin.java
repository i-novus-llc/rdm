package ru.i_novus.ms.rdm.impl.model.field;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "id", visible=true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanField.class, name = "BooleanField"),
        @JsonSubTypes.Type(value = DateField.class, name = "DateField"),
        @JsonSubTypes.Type(value = FloatField.class, name = "FloatField"),
        @JsonSubTypes.Type(value = IntegerField.class, name = "IntegerField"),
        @JsonSubTypes.Type(value = IntegerStringField.class, name = "IntegerStringField"),
        @JsonSubTypes.Type(value = ListField.class, name = "ListField"),
        @JsonSubTypes.Type(value = ReferenceField.class, name = "ReferenceField"),
        @JsonSubTypes.Type(value = StringField.class, name = "StringField"),
        @JsonSubTypes.Type(value = TreeField.class, name = "TreeField")
})
public class VdsFieldMixin {
}
