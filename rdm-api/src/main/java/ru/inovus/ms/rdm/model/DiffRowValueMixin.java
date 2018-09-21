package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;

import java.util.List;

public abstract class DiffRowValueMixin {

    @JsonCreator
    public DiffRowValueMixin(@JsonProperty("values") List<DiffFieldValue> values, @JsonProperty("status") DiffStatusEnum status) {}

}
