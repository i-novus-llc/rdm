package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AfterUpdateDataStrategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

@Component
@SuppressWarnings({"rawtypes", "java:S3740","java:S1172"})
public class DefaultAfterUpdateDataStrategy implements AfterUpdateDataStrategy {

    @Override
    public void apply(RefBookVersionEntity entity,
                      List<RowValue> addedRowValues,
                      List<RowValue> oldRowValues,
                      List<RowValue> newRowValues) {
        // Nothing to do.
    }
}
