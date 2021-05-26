package ru.i_novus.ms.rdm.impl.strategy.data;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

@SuppressWarnings("java:S3740")
public interface AddRowValuesStrategy extends Strategy {

    void add(RefBookVersionEntity entity, List<RowValue> rowValues);
}
