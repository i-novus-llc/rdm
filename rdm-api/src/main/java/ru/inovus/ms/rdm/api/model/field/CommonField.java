package ru.inovus.ms.rdm.api.model.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

public class CommonField extends Field {

    public CommonField() {
        super(null);
    }

    public CommonField(String name) {
        super(name);
    }

    @JsonIgnore
    @Override
    public String getType() {
        return null;
    }

    @Override
    public FieldValue valueOf(Object value) {
        return null;
    }

}
