package ru.i_novus.ms.rdm.api.model.diff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Field;

public class DiffFieldValueMixin {

    @JsonCreator
    public DiffFieldValueMixin(@JsonProperty("field") Field field,
                                 @JsonProperty("oldValue") Object oldValue,
                                 @JsonProperty("newValue") Object newValue,
                                 @JsonProperty("status") DiffStatusEnum status) {}
}
