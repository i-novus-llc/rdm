package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AfterUpdateDataStrategy;
import ru.i_novus.ms.rdm.impl.strategy.publish.EditPublishStrategy;
import ru.i_novus.ms.rdm.impl.strategy.referrer.UnversionedAfterUpdateProcessReferrersStrategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UnversionedAfterUpdateDataStrategy implements AfterUpdateDataStrategy {

    @Autowired
    @Qualifier("unversionedEditPublishStrategy")
    private EditPublishStrategy editPublishStrategy;

    @Autowired
    private UnversionedAfterUpdateProcessReferrersStrategy processReferrersStrategy;

    @Override
    public void apply(RefBookVersionEntity entity,
                      List<RowValue> addedRowValues,
                      List<RowValue> oldRowValues,
                      List<RowValue> newRowValues) {

        if (isEmpty(addedRowValues) && isEmpty(oldRowValues) && isEmpty(newRowValues))
            return;

        editPublishStrategy.publish(entity);

        processReferrersStrategy.apply(entity, addedRowValues, oldRowValues, newRowValues);
    }
}
