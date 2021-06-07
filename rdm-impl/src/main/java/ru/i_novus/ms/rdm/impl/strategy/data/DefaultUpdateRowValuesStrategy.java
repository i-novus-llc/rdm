package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.util.ErrorUtil;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.List;

@Component
@SuppressWarnings({"rawtypes", "java:S3740","java:S1172"})
public class DefaultUpdateRowValuesStrategy implements UpdateRowValuesStrategy {

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private DraftDataService draftDataService;

    @Override
    public void update(RefBookVersionEntity entity, List<RowValue> oldRowValues, List<RowValue> newRowValues) {

        before(entity, oldRowValues, newRowValues);

        try {
            draftDataService.updateRows(entity.getStorageCode(), newRowValues);

        } catch (RuntimeException e) {
            ErrorUtil.rethrowError(e);
        }

        after(entity, oldRowValues, newRowValues);
    }

    protected void before(RefBookVersionEntity entity, List<RowValue> oldRowValues, List<RowValue> newRowValues) {

        List<Long> systemIds = RowUtils.toSystemIds(newRowValues);
        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(entity.getId(), systemIds);
    }

    protected void after(RefBookVersionEntity entity, List<RowValue> oldRowValues, List<RowValue> newRowValues) {

        // Nothing to do.
    }

    public RefBookConflictRepository getConflictRepository() {
        return conflictRepository;
    }

    public DraftDataService getDraftDataService() {
        return draftDataService;
    }
}
