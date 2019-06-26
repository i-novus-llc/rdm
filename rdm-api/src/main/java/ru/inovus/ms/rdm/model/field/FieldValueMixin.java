package ru.inovus.ms.rdm.model.field;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.i_novus.platform.datastorage.temporal.model.value.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanFieldValue.class, name = "BooleanFieldValue"),
        @JsonSubTypes.Type(value = DateFieldValue.class, name = "DateFieldValue"),
        @JsonSubTypes.Type(value = FloatFieldValue.class, name = "FloatFieldValue"),
        @JsonSubTypes.Type(value = IntegerFieldValue.class, name = "IntegerFieldValue"),
        @JsonSubTypes.Type(value = ReferenceFieldValue.class, name = "ReferenceFieldValue"),
        @JsonSubTypes.Type(value = StringFieldValue.class, name = "StringFieldValue"),
        @JsonSubTypes.Type(value = TreeFieldValue.class, name = "TreeFieldValue")
})
public class FieldValueMixin {

}
