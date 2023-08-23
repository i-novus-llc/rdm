package ru.i_novus.ms.rdm.api.model.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

import java.io.Serializable;

@SuppressWarnings({"rawtypes", "java:S3740"})
public class CommonField extends Field {

    public CommonField() {
        super();
    }

    public CommonField(String name) {
        super(name);
    }

    @JsonIgnore
    @Override
    public String getType() {
        return null;
    }

    @JsonIgnore
    @Override
    public Class getFieldValueClass() {
        return null;
    }

    @Override
    public FieldValue valueOf(Serializable value) {
        return null;
    }

}
