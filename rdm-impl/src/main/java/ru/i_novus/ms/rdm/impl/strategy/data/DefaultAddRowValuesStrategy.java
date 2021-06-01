package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.util.ErrorUtil;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.List;

@Component
@SuppressWarnings({"rawtypes", "java:S3740","java:S1172"})
public class DefaultAddRowValuesStrategy implements AddRowValuesStrategy {

    @Autowired
    private DraftDataService draftDataService;

    @Override
    public void add(RefBookVersionEntity entity, List<RowValue> rowValues) {

        before(entity, rowValues);

        try {
            draftDataService.addRows(entity.getStorageCode(), rowValues);

        } catch (RuntimeException e) {
            ErrorUtil.rethrowError(e);
        }

        after(entity, rowValues);
    }

    protected void before(RefBookVersionEntity entity, List<RowValue> rowValues) {

        // Nothing to do.
    }

    protected void after(RefBookVersionEntity entity, List<RowValue> rowValues) {

        // Nothing to do.
    }
}
