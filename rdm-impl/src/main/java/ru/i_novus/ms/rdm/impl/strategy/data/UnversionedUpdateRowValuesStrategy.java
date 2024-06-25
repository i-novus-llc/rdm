package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.UpdateRowValuesStrategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedUpdateRowValuesStrategy implements UpdateRowValuesStrategy {

    @Autowired
    @Qualifier("defaultUpdateRowValuesStrategy")
    private UpdateRowValuesStrategy updateRowValuesStrategy;

    @Override
    public void update(RefBookVersionEntity entity, List<RowValue> oldRowValues, List<RowValue> newRowValues) {

        updateRowValuesStrategy.update(entity, oldRowValues, newRowValues);
    }
}
