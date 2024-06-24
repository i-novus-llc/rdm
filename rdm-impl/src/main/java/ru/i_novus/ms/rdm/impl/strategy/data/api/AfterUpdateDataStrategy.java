package ru.i_novus.ms.rdm.impl.strategy.data.api;

import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

@SuppressWarnings({"rawtypes", "java:S3740"})
public interface AfterUpdateDataStrategy extends Strategy {

    void apply(RefBookVersionEntity entity,
               List<RowValue> addedRowValues,
               List<RowValue> oldRowValues,
               List<RowValue> newRowValues);
}
