package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AddRowValuesStrategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedAddRowValuesStrategy implements AddRowValuesStrategy {

    @Autowired
    @Qualifier("defaultAddRowValuesStrategy")
    private AddRowValuesStrategy addRowValuesStrategy;

    @Override
    public void add(RefBookVersionEntity entity, List<RowValue> rowValues) {

        addRowValuesStrategy.add(entity, rowValues);
    }
}
