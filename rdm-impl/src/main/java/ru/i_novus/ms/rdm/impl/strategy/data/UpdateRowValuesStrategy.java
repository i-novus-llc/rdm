package ru.i_novus.ms.rdm.impl.strategy.data;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

@SuppressWarnings({"rawtypes", "java:S3740"})
public interface UpdateRowValuesStrategy extends Strategy {

    void update(RefBookVersionEntity entity, List<RowValue> oldRowValues, List<RowValue> newRowValues);
}
